import mdoc.internal.cli.InputFile
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.{CodeFence, MarkdownFile}
import sbt.plugins.JvmPlugin
import sbt._

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, StandardOpenOption}
import scala.collection.JavaConverters._
import scala.meta.inputs.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.{AbsolutePath, RelativePath}

object MdToSourcePlugin extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val mdDir = settingKey[File]("MD Source Dir")
    lazy val examplesDir = settingKey[File]("Examples Dir")
    lazy val generateExamples = taskKey[Unit]("generate examples")
  }

  import autoImport._

  val generateExamplesTask = Def.task {

    if (examplesDir.value.exists()) {
      Files.walk(examplesDir.value.toPath).iterator().asScala.toSeq.reverse.foreach(_.toFile.delete())
    }
    examplesDir.value.mkdirs()

    mdDir.value.listFiles().filter(_.ext == "md").foreach { file =>
      val chapterName = file.getName.replaceFirst("^\\d\\d_", "").stripSuffix(".md")
      val outFile = examplesDir.value / (chapterName + ".scala")
      val inputFile = InputFile(RelativePath(file), AbsolutePath(file), AbsolutePath(outFile), AbsolutePath(mdDir.value), AbsolutePath(examplesDir.value))

      val source = FileIO.slurp(AbsolutePath(file), Charset.defaultCharset())
      val input = Input.VirtualFile(file.absolutePath, source)

      val reporter = ConsoleReporter.default
      val md = MarkdownFile.parse(input, inputFile, reporter)
      val codeBlocks = md.parts.collect {
        case codeFence: CodeFence =>
          codeFence.body.value
      }

      if (codeBlocks.nonEmpty) {
        val contents = Seq(s"package $chapterName", "") ++ codeBlocks.flatMap { block =>
          Seq(block, "")
        }

        Files.write(outFile.toPath, contents.asJava, StandardOpenOption.CREATE)
      }
    }

  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    generateExamples := generateExamplesTask.value
  )

}

