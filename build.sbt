import java.io.File
import java.nio.file.{Files, Path}

name := "EffectOrientedProgramming"

val zioVersion = "2.0.0-RC1"

lazy val commonSettings = Seq(
  scalaVersion := "3.1.0",

  scalacOptions += "-Yexplicit-nulls",
  scalacOptions -= "-explain-types",
  scalacOptions -= "-explain",

  // TODO Make sure this only happens in Intellij. It breaks VSCode
  // scalacOptions -= "-encoding"

  libraryDependencies ++= Seq(
    "dev.zio" %% "zio"          % zioVersion,
    "dev.zio" %% "zio-test"     % zioVersion,
    "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    "dev.zio" %% "zio-prelude"  % "1.0.0-RC9"
  ),

  testFrameworks +=
    new TestFramework(
      "zio.test.sbt.ZTestFramework"
    ),

  // fork := true,
)

lazy val booker = (project in file("booker")).settings(commonSettings).enablePlugins(GraalVMNativeImagePlugin)
lazy val experiments = (project in file("experiments"))
.settings(commonSettings).settings(fork:=true)
lazy val rube = (project in file("rube")).settings(commonSettings)

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file(".")).settings(commonSettings).enablePlugins(MdocPlugin).aggregate(booker, experiments, rube)

mdocIn := file("Chapters")

mdocOut := file("manuscript")

// windows sometimes produces OverlappingFileLockException
scalafmtOnCompile := (!System.getProperty("os.name").toLowerCase.contains("win"))

Compile / packageDoc / publishArtifact := false

Compile / doc / sources := Seq.empty

lazy val bookTxt = taskKey[Unit]("Create the Book.txt")

bookTxt := {

  import scala.util.Try
  import scala.collection.JavaConverters._

  val files = Files.list(mdocIn.value.toPath).iterator().asScala

  def chapterNum(path: Path): Option[Int] = {
    val justFile = path.toFile.getName.split(File.pathSeparator).last
    justFile.split('_').headOption.flatMap { firstPart =>
      Try(firstPart.toInt).toOption
    }
  }

  val chapters = files.flatMap { f =>
    if (f.toFile.ext == "md") {
      chapterNum(f).map(_ -> f)
    }
    else
    {
      None
    }
  }.toSeq.sortBy(_._1).map(_._2.toFile.getName.stripPrefix(mdocIn.value.getName).stripPrefix(File.pathSeparator))

  val bookTxtPath = mdocOut.value / "Book.txt"
  mdocOut.value.mkdir()
  Files.write(bookTxtPath.toPath, chapters.asJava)
}

mdoc := mdoc.dependsOn(bookTxt).evaluated


// MdToSourcePlugin

// note: these are prefixed with _root_ to avoid a conflict with the mdoc value
import _root_.mdoc.internal.cli.InputFile
import _root_.mdoc.internal.io.ConsoleReporter
import _root_.mdoc.internal.markdown.{CodeFence, MarkdownFile}

import java.io.{File, FilenameFilter}
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import java.util.stream.Collectors
import scala.collection.JavaConverters._
import scala.meta.inputs.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.{AbsolutePath, RelativePath}

lazy val mdDir = settingKey[File]("MD Source Dir")
lazy val examplesDir = settingKey[File]("Examples Dir")
lazy val generateExamples = taskKey[Unit]("generate examples")



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

generateExamples := generateExamplesTask.value

mdDir := file("Chapters")

examplesDir := file("Examples/src/main/scala")
