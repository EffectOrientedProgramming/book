import mdoc.internal.cli.InputFile
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.{CodeFence, MarkdownFile}
import sbt.plugins.JvmPlugin
import sbt._

import java.io.{File, FilenameFilter}
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.stream.Collectors
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

  def deleteAllScalaFilesRecursively(file: File): Unit = {
    val onlyScala: FilenameFilter = (_: File, name: String) => name.endsWith(".scala")
    file.listFiles(onlyScala).toSeq.foreach(_.delete())
    file.listFiles().foreach( file => if(file.isDirectory) deleteAllScalaFilesRecursively(file) )
    if  (file.isDirectory && file.listFiles().isEmpty) file.delete()
  }

  val generateExamplesTask = Def.task {

    if (examplesDir.value.exists()) {
      deleteAllScalaFilesRecursively(examplesDir.value)
    }

    import java.nio.file.StandardCopyOption._

    def copy(source: Path , dest: Path ): Unit = {
      println("Source: " + source)
      println("dest: " + dest)
      Files.copy(source, dest, REPLACE_EXISTING)
    }

    import scala.jdk.CollectionConverters._
    def copyFolder(src: Path , dest: Path ): Unit = {
      import java.nio.file.{Files, Paths}
      import scala.collection.JavaConverters._

      scalaFileWalk(src).foreach(
        source => copy(source, dest.resolve(src.relativize(source)))
      )
    }

    def scalaFileWalk(src: Path): List[Path] = {
      val currentLevelFiles = Files.list(src).iterator().asScala.toList
      currentLevelFiles ++ currentLevelFiles.flatMap(file =>
        if (file.toFile.isDirectory)
          scalaFileWalk(file)
        else List.empty
      )
    }

    examplesDir.value.mkdirs()
    copyFolder(Paths.get(".").resolve("src").resolve("main").resolve("scala"), examplesDir.value.toPath)


    def isChapter(f: File): Boolean = {
      f.name.matches("^\\d\\d_.*")
    }

    mdDir.value.listFiles().filter(_.ext == "md").filter(isChapter).foreach { file =>
      val chapterName = file.getName.stripSuffix(".md") //replaceFirst("^\\d\\d_", "")
      val outFile = examplesDir.value / (chapterName + ".scala")
      val inputFile = InputFile(RelativePath(file), AbsolutePath(file), AbsolutePath(outFile), AbsolutePath(mdDir.value), AbsolutePath(examplesDir.value))

      val source = FileIO.slurp(AbsolutePath(file), Charset.defaultCharset())
      val input = Input.VirtualFile(file.absolutePath, source)

      val reporter = ConsoleReporter.default
      val md = MarkdownFile.parse(input, inputFile, reporter)
      val codeBlocks = md.parts.collect {
        case codeFence: CodeFence if codeFence.info.value.startsWith("scala mdoc") && !codeFence.info.value.startsWith("scala mdoc:nest") =>
          codeFence.body.value
      }

      if (codeBlocks.nonEmpty) {
        val header = Seq(s"package `$chapterName`", "", "@main def run() = ")

        val indentedBlocks = codeBlocks.flatMap { block =>
          block.linesIterator.map("  " + _).toList :+ ""
        }

        val contents = header ++ indentedBlocks

        Files.write(outFile.toPath, contents.asJava, StandardOpenOption.CREATE)
      }
    }

  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    generateExamples := generateExamplesTask.value
  )

}

