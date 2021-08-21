import com.vladsch.flexmark.util.data.MutableDataSet
import mdoc.internal.cli.{InputFile, Settings}
import mdoc.internal.markdown.{CodeFence, Markdown, MarkdownFile}
import sbt.Keys._
import sbt.{Def, _}
import sbt.plugins.JvmPlugin

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, OpenOption, StandardOpenOption}
import mdoc.Reporter
import mdoc.internal.io.ConsoleReporter

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
    mdDir.value.listFiles().filter(_.ext == "md").foreach { file =>
      println(file)
      val outFile = examplesDir.value / file.getName.replace(".md", ".scala")
      val inputFile = InputFile(RelativePath(file), AbsolutePath(file), AbsolutePath(outFile), AbsolutePath(mdDir.value), AbsolutePath(examplesDir.value))

      val source = FileIO.slurp(AbsolutePath(file), Charset.defaultCharset())
      val input = Input.VirtualFile(file.absolutePath, source)

      val reporter = ConsoleReporter.default
      /*
      val markdownSettings = new MutableDataSet()

      val settings = Settings.default(AbsolutePath(file))
      val md = Markdown.toMarkdown(input, markdownSettings, reporter, settings)
      */
      val md = MarkdownFile.parse(input, inputFile, reporter)
      val codeBlocks = md.parts.collect {
        case codeFence: CodeFence =>
          codeFence.body.value
      }.flatMap { block =>
        Seq(block, "")
      }

      examplesDir.value.delete()
      examplesDir.value.mkdir()
      Files.write(outFile.toPath, codeBlocks.asJava, StandardOpenOption.CREATE)
    }

  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    generateExamples := generateExamplesTask.value
  )

  //generateExamples := generateExamples.value

  /*

  def ammoniteBookBits = {
    // TODO Consolidate with non-ammonite script pieces below
    println("Generating a fresh Book.txt file")

    /* What do we need for the full book process?
      1. Get all .md files
      2. concatenate them together
     */
    import ammonite.ops._
    val eopDir = pwd
    val markdownDir = eopDir / "Chapters"
    val bookTxt = markdownDir / "Book.txt"
    val markdownFiles = (ls! markdownDir).filter(_.last.endsWith(".md"))

    write.over(bookTxt, "")

    markdownFiles.foreach {
      mdFile =>
        write.append(bookTxt, mdFile.last + "\n")
    }
  }

  lazy val generateSourcesTask = Def.task {
    ammoniteBookBits
    /*
      TODO Decide when appropriate to remove completely
    (MdToSource / mdDirectory).value
      .listFiles(FileFilter.globFilter("*.md"))
      .flatMap { md =>
        val contents: String = IO.read(md)
        val codeFiles =
          FenceFinder.findNamedCodeBlocksInFile(contents, md.getCanonicalPath)
        //      IO.write(md,
        //        ContentRules.digestContentsAndApplyUpdatesInPlace(contents, md.getCanonicalPath)
        //      )
        codeFiles.map { case NamedCodeBlock(name, contents) =>
          val file = (MdToSource / sourceManaged).value / name
          IO.write(file, contents.content.mkString("\n").concat("\n"))
          file
        }
      }
      .toSeq
     */
    Seq()

  }

  def alterExamplesInPlace(md: File) = {
    val contents: String = IO.read(md)
    if (
      !md.name.contains("Java_Interoperability")
    ) // If we don't check this, we start hitting Java code that should _not_ be converted
      IO.write(
        md,
        contents.linesIterator.flatMap(KotlinConversion.convert).mkString("\n")
      )
  }

  override lazy val projectSettings = Seq(
    Compile / sourceGenerators += generateSourcesTask.taskValue,
//    Compile / watchSources += file(
//      (MdToSource / mdDirectory).value.getAbsolutePath
//    )
  )

   */

}

