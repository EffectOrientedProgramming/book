import sbt.IO

import java.io.File
import java.nio.file.Files
import sbt._
import Keys._
import java.io.File
import java.nio.file.{Files, Path, Paths}

case class FencedCode(content: String)

case class ProseFile(p: Path):
  val cleanName =
    val fileNameRaw =
      p.toFile
        .getName
        .toLowerCase
        .stripSuffix(".md")
    if (fileNameRaw.contains("_"))
      fileNameRaw.dropWhile(_ != '_').drop(1)
    else
      fileNameRaw

object BuildTooling:

  def appendExperimentsToEndOfBookInNewChapter(
      packagedName: String,
      manuscript: File,
      experiments: List[FencedCode]
  ): Unit =
    val packageMarkdownFileName =
      packagedName
        .replaceAllLiterally("/", "-") + ".md"

    val nf = manuscript / packageMarkdownFileName
    println(
      "Adding standalone example to end of book: " +
        nf.toString
    )

    nf.getParentFile.mkdirs()

    val md =
      s"""## ${packageMarkdownFileName
          .stripSuffix(".md")}
         |
         | ${experiments.map(_.content).mkString}
         |""".stripMargin

    Files.write(nf.toPath, md.getBytes)

    def addChapterToLeanpubIndex(
        chapterName: String
    ): Unit =
      IO.append(
        manuscript / "Book.txt",
        chapterName + "\n"
      )

    addChapterToLeanpubIndex(
      packageMarkdownFileName
    )
  end appendExperimentsToEndOfBookInNewChapter

  def appendExperimentsToMatchingProseFile(
      proseFile: ProseFile,
      experiments: List[FencedCode]
  ) =
    println(
      "Should append to existing file for: " +
        proseFile
    )
    val chapterExperiments =
      s"""
         |
         |## Automatically attached experiments.
         | These are included at the end of this
         | chapter because their package in the
         | experiments directory matched the name
         | of this chapter. Enjoy working on the
         | code with full editor capabilities :D
         |
         | ${experiments.map(_.content).mkString}
         |""".stripMargin
    IO.append(
      proseFile.p.toFile,
      chapterExperiments
    )
  end appendExperimentsToMatchingProseFile

  // note: these are prefixed with _root_ to
  // avoid a conflict with the mdoc value
  import mdoc.internal.cli.InputFile
  import mdoc.internal.io.ConsoleReporter
  import mdoc.internal.markdown.{
    CodeFence,
    MarkdownFile
  }

  import java.io.{File, FilenameFilter}
  import java.nio.charset.Charset
  import java.nio.file.{
    Files,
    Path,
    Paths,
    StandardOpenOption
  }

  import java.util.stream.Collectors
  import scala.collection.JavaConverters._
  import scala.meta.inputs.Input
  import scala.meta.internal.io.FileIO
  import scala.meta.io.{
    AbsolutePath,
    RelativePath
  }

  lazy val mdDir =
    settingKey[File]("MD Source Dir")
  lazy val examplesDir =
    settingKey[File]("Examples Dir")
  lazy val generateExamples =
    taskKey[Unit]("generate examples")

  val generateExamplesTask =
    Def.task {

      def deleteAllScalaFilesRecursively(
          file: File
      ): Unit =
        val onlyScala: FilenameFilter =
          (_: File, name: String) =>
            name.endsWith(".scala")
        file
          .listFiles(onlyScala)
          .toSeq
          .foreach(_.delete())
        file
          .listFiles()
          .foreach(file =>
            if (file.isDirectory)
              deleteAllScalaFilesRecursively(
                file
              )
          )
        if (
          file.isDirectory &&
          file.listFiles().isEmpty
        )
          file.delete()
      end deleteAllScalaFilesRecursively

      if (examplesDir.value.exists()) {
        deleteAllScalaFilesRecursively(
          examplesDir.value
        )
      }

      import java.nio.file.StandardCopyOption._

      def copy(source: Path, dest: Path): Unit =
        println("Source: " + source)
        println("dest: " + dest)
        Files
          .copy(source, dest, REPLACE_EXISTING)

      import scala.jdk.CollectionConverters._
      def copyFolder(
          src: Path,
          dest: Path
      ): Unit =
        import java.nio.file.{Files, Paths}
        import scala.collection.JavaConverters._

        scalaFileWalk(src).foreach(source =>
          copy(
            source,
            dest.resolve(src.relativize(source))
          )
        )

      def scalaFileWalk(src: Path): List[Path] =
        val currentLevelFiles =
          Files
            .list(src)
            .iterator()
            .asScala
            .toList
        currentLevelFiles ++
          currentLevelFiles.flatMap(file =>
            if (file.toFile.isDirectory)
              scalaFileWalk(file)
            else
              List.empty
          )

      examplesDir.value.mkdirs()
      copyFolder(
        Paths
          .get(".")
          .resolve("src")
          .resolve("main")
          .resolve("scala"),
        examplesDir.value.toPath
      )

      def isChapter(f: File): Boolean =
        f.name.matches("^\\d\\d_.*")

      mdDir
        .value
        .listFiles()
        .filter(_.ext == "md")
        .filter(isChapter)
        .foreach { file =>
          val chapterName =
            file
              .getName
              .stripSuffix(
                ".md"
              ) // replaceFirst("^\\d\\d_", "")
          val outFile =
            examplesDir.value /
              (chapterName + ".scala")
          val inputFile =
            InputFile(
              RelativePath(file),
              AbsolutePath(file),
              AbsolutePath(outFile),
              AbsolutePath(mdDir.value),
              AbsolutePath(examplesDir.value)
            )

          val source =
            FileIO.slurp(
              AbsolutePath(file),
              Charset.defaultCharset()
            )
          val input =
            Input.VirtualFile(
              file.absolutePath,
              source
            )

          val reporter = ConsoleReporter.default
          val md =
            MarkdownFile
              .parse(input, inputFile, reporter)
          val codeBlocks =
            md.parts
              .collect {
                case codeFence: CodeFence
                    if codeFence
                      .info
                      .value
                      .startsWith(
                        "scala mdoc"
                      ) &&
                      !codeFence
                        .info
                        .value
                        .startsWith(
                          "scala mdoc:nest"
                        ) =>
                  codeFence.body.value
              }

          if (codeBlocks.nonEmpty) {
            val header =
              Seq(
                s"package `$chapterName`",
                "",
                "@main def run() = "
              )

            val indentedBlocks =
              codeBlocks.flatMap { block =>
                block
                  .linesIterator
                  .map("  " + _)
                  .toList :+ ""
              }

            val contents =
              header ++ indentedBlocks

            Files.write(
              outFile.toPath,
              contents.asJava,
              StandardOpenOption.CREATE
            )
          }
        }

    }

  def generateBookTxtFromNumberedChapters(
      proseDirectory: File,
      leanPubDirectory: File
  ): Path =
    import scala.util.Try
    import scala.collection.JavaConverters._

    val files =
      Files
        .list(proseDirectory.toPath)
        .iterator()
        .asScala

    def chapterNum(path: Path): Option[Int] =
      val justFile =
        path
          .toFile
          .getName
          .split(File.pathSeparator)
          .last
      justFile
        .split('_')
        .headOption
        .flatMap { firstPart =>
          Try(firstPart.toInt).toOption
        }

    val chapters =
      files
        .flatMap { f =>
          if (f.toFile.ext == "md") {
            chapterNum(f).map(_ -> f)
          } else
            None
        }
        .toSeq
        .sortBy(_._1)
        .map(
          _._2
            .toFile
            .getName
            .stripPrefix(proseDirectory.getName)
            .stripPrefix(File.pathSeparator)
        )

    leanPubDirectory.mkdir()
    val bookTxtPath =
      leanPubDirectory / "Book.txt"
    Files
      .write(bookTxtPath.toPath, chapters.asJava)
  end generateBookTxtFromNumberedChapters

  def produceLeanpubManuscript(
      leanPubDirectory: File
  ) =

    import scala.jdk.CollectionConverters._

    def filesIn(p: Path) =
      Files.walk(p).iterator().asScala.toList

    case class ExperimentFile(p: Path)

    val experimentClasses
        : Map[String, List[ExperimentFile]] =
      filesIn(file("experiments/src").toPath)
        .filter(_.toFile.ext == "scala")
        .map(ExperimentFile)
        .groupBy { file =>
          file.p.getParent.toString
        }

    val nf =
      leanPubDirectory / "ExperimentsSection.md"
    val experimentsHeaderContent =
      "# Experiments\n\n" +
        "These experiments are not currently attached to a chapter, but are included for previewing. Before publication, we should not have any lingering experiments here.\n\n"
    IO.append(
      leanPubDirectory / "Book.txt",
      nf.getName + "\n"
    )

    val proseFiles: Seq[ProseFile] =
      filesIn(leanPubDirectory.toPath)
        .filter(_.toFile.getName.endsWith(".md"))
        .sortBy(_.toFile.getName)
        .map(ProseFile)

    experimentClasses.foreach {
      case (dir, dirFiles) =>
        val packagedName: String =
          dir.stripPrefix(
            "experiments/src/main/scala/"
          )

        val proseFileOnSameTopic
            : Option[ProseFile] =
          proseFiles
            .find(_.cleanName == packagedName)

        def fileFence(
            experimentFile: ExperimentFile
        ) =
          val file  = experimentFile.p.toFile
          val lines = IO.read(file)
          FencedCode(s"""
               |
               |### ${file.toString}
               |```scala
               |$lines
               |```
               |""".stripMargin)

        val allFences: List[FencedCode] =
          dirFiles
            .sortBy(_.p.getFileName.toString)
            .map(fileFence)

        proseFileOnSameTopic match
          case Some(value) =>
            appendExperimentsToMatchingProseFile(
              value,
              allFences
            )
          case None => {
            appendExperimentsToEndOfBookInNewChapter(
              packagedName,
              leanPubDirectory,
              allFences
            )
          }
    }
  end produceLeanpubManuscript

  def experimentLibrariesThatNeedToBeVettedForInclusionInBook =
    Seq(
      "org.apache.commons" % "commons-text" %
        "1.10.0",
      "io.github.scottweaver" %%
        "zio-2-0-testcontainers-postgresql" %
        "0.9.0",
      "io.github.scottweaver"        %%
        "zio-2-0-db-migration-aspect" % "0.9.0",
      "io.getquill" %% "quill-jdbc-zio" %
        "4.6.0.1",
      "io.getquill" %% "quill-zio"   % "4.6.0.1",
      "dev.zio"     %% "zio-process" % "0.7.1"
    )

  // TODO Make a Versions object?
  val zioVersion = "2.0.10"

  lazy val commonSettings =
    Seq(
      libraryDependencies ++=
        Seq(
          "dev.zio" %% "zio"       % zioVersion,
          "dev.zio" %% "zio-cache" % "0.2.2",
          "dev.zio" %% "zio-concurrent" %
            zioVersion,
          "dev.zio" %% "zio-logging" % "2.1.8",
          "dev.zio" %% "zio-streams" %
            zioVersion,
          "dev.zio"   %% "zio-test" % zioVersion,
          "dev.zio"   %% "zio-test-sbt" %
            zioVersion % Test,
          "dev.zio"   %% "zio-prelude" %
            "1.0.0-RC18"
        ),
      scalaVersion := "3.2.2",
      scalacOptions -= "-explain-types",
      scalacOptions -= "-explain"
    )
end BuildTooling
