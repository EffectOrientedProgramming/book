import sbt.IO

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

    val md: String =
      s"""## ${packageMarkdownFileName
          .stripSuffix(".md")}
         |
         | ${experiments.map(_.content).mkString}
         |""".stripMargin

    FileIOBullshit.createFile(nf, Seq(md))

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
  ): Unit =
//    println("Should append to existing file for: " + proseFile)
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

  import java.io.File
  import java.nio.charset.Charset
  import java.nio.file.{
    Files,
    Path,
    Paths,
    StandardOpenOption
  }

  lazy val mdDir =
    settingKey[File]("MD Source Dir")
  lazy val examplesDir =
    settingKey[File]("Examples Dir")
  lazy val generateExamples =
    taskKey[Unit]("generate examples")

  val generateExamplesTask =
    Def.task {
      generateExamplesLogic(
        examplesDir.value,
        mdDir.value
      )
    }

  private def generateExamplesLogic(
      examplesDirectory: File,
      markdownDir: File
  ): Unit =
    if (examplesDirectory.exists())
      FileIOBullshit
        .deleteAllScalaFilesRecursively(
          examplesDirectory
        )
    else
      examplesDirectory.mkdirs()

    FileIOBullshit.copyFolder(
      Paths
        .get(".")
        .resolve("src")
        .resolve("main")
        .resolve("scala"),
      examplesDirectory.toPath
    )

    def isChapter(f: File): Boolean =
      f.name.matches("^\\d\\d_.*")

    import scala.meta.inputs.Input
    import scala.meta.internal.io.FileIO
    import scala.meta.io.{
      AbsolutePath,
      RelativePath
    }
    import mdoc.internal.cli.InputFile
    import mdoc.internal.io.ConsoleReporter
    import mdoc.internal.markdown.{
      CodeFence,
      MarkdownFile
    }

    FileIOBullshit
      .markdownFilesInFile(markdownDir)
      .filter(isChapter)
      .foreach { file =>
        val chapterName =
          file.getName.stripSuffix(".md")
        val outFile =
          examplesDirectory /
            (chapterName + ".scala")
        val inputFile =
          InputFile(
            RelativePath(file),
            AbsolutePath(file),
            AbsolutePath(outFile),
            AbsolutePath(markdownDir),
            AbsolutePath(examplesDirectory)
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

        val codeBlocks =
          MarkdownFile
            .parse(
              input,
              inputFile,
              ConsoleReporter.default
            )
            .parts
            .collect {
              case codeFence: CodeFence
                  if codeFence
                    .info
                    .value
                    .startsWith("scala mdoc") &&
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

          val contents = header ++ indentedBlocks

          FileIOBullshit
            .createFile(outFile, contents)
        }
      }
  end generateExamplesLogic

  def generateBookTxtFromNumberedChapters(
      proseDirectory: File,
      leanPubDirectory: File
  ): Path =
    import scala.util.Try

    val files =
      FileIOBullshit
        .markdownFilesIn(proseDirectory.toPath)

    case class NumberedChapter(
        number: Int,
        content: Path
    )

    def parseNumberedChapter(
        path: Path
    ): Option[NumberedChapter] =
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
          Try(
            NumberedChapter(
              firstPart.toInt,
              path
            )
          ).toOption
        }
    end parseNumberedChapter

    val chapters =
      files
        .flatMap(parseNumberedChapter)
        .sortBy(chapter => chapter.number)
        .map(chapter =>
          chapter.content.getFileName.toString
        )

    val bookTxtPath =
      leanPubDirectory / "Book.txt"

    FileIOBullshit
      .createFile(bookTxtPath, chapters)
  end generateBookTxtFromNumberedChapters

  def produceLeanpubManuscript(
      leanPubDirectory: File
  ): Unit =
    // Files that we have actually written
    // content for
    // They will be used as anchors for attaching
    // experiments
    val proseFiles: Seq[ProseFile] =
      FileIOBullshit
        .markdownFilesIn(leanPubDirectory.toPath)
        .sortBy(_.toFile.getName)
        .map(ProseFile)

    def packageName(
        experimentFile: ExperimentFile
    ) =
      experimentFile
        .p
        .getParent
        .toString
        .stripPrefix(
          "experiments/src/main/scala/"
        )

    // These may or may not correspond to prose
    // chapters
    val experimentClasses
        : Map[String, List[ExperimentFile]] =
      FileIOBullshit
        .scalaFilesIn(
          file("experiments/src").toPath
        )
        .map(ExperimentFile)
        // TODO Investigate nested package
        // behavior
        .groupBy(packageName)

    val nf =
      leanPubDirectory / "ExperimentsSection.md"
    val experimentsHeaderContent =
      "# Experiments\n\n" +
        "These experiments are not currently attached to a chapter, but are included for previewing. Before publication, we should not have any lingering experiments here.\n\n"
    IO.append(
      leanPubDirectory / "Book.txt",
      nf.getName + "\n"
    )
    FileIOBullshit.createFile(
      nf,
      Seq(experimentsHeaderContent)
    )

    experimentClasses.foreach {
      case (packageName, experimentsInPackage) =>
        val proseFileOnSameTopic
            : Option[ProseFile] =
          proseFiles
            .find(_.cleanName == packageName)

        val allFences: List[FencedCode] =
          experimentsInPackage
            .sortBy(_.p.getFileName.toString)
            .map(fileFence)

        proseFileOnSameTopic match
          case Some(value) =>
            appendExperimentsToMatchingProseFile(
              value,
              allFences
            )
          case None =>
            appendExperimentsToEndOfBookInNewChapter(
              packageName,
              leanPubDirectory,
              allFences
            )
    }
  end produceLeanpubManuscript

  case class ExperimentFile(p: Path)

  private def fileFence(
      experimentFile: ExperimentFile
  ): FencedCode =
    val file  = experimentFile.p.toFile
    val lines = IO.read(file)
    FencedCode(s"""
         |
         |### ${file.toString}
         |```scala
         |$lines
         |```
         |""".stripMargin)

  object FileIOBullshit:
    import java.nio.file.StandardCopyOption._
    import scala.collection.JavaConverters._

    def createFile(
        target: File,
        contents: Seq[String]
    ): Path =
      target.getParentFile.mkdirs()
      Files.write(
        target.toPath,
        contents.asJava,
        StandardOpenOption.CREATE
      )

    def copy(source: Path, dest: Path): Unit =
      Files.copy(source, dest, REPLACE_EXISTING)

    def copyFolder(src: Path, dest: Path): Unit =
      scalaFileWalk(src).foreach(source =>
        FileIOBullshit.copy(
          source,
          dest.resolve(src.relativize(source))
        )
      )

    def scalaFileWalk(src: Path): List[Path] =
      val currentLevelFiles =
        Files.list(src).iterator().asScala.toList
      currentLevelFiles ++
        currentLevelFiles.flatMap(file =>
          if (file.toFile.isDirectory)
            scalaFileWalk(file)
          else
            List.empty
        )

    def deleteAllScalaFilesRecursively(
        file: File
    ): Unit =
      import java.io.{File, FilenameFilter}
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
            deleteAllScalaFilesRecursively(file)
        )
      if (
        file.isDirectory &&
        file.listFiles().isEmpty
      )
        file.delete()
    end deleteAllScalaFilesRecursively

    def filesIn(p: Path): List[Path] =
      Files.walk(p).iterator().asScala.toList

    def markdownFilesIn(p: Path): List[Path] =
      filesIn(p).filter(_.toFile.ext == "md")

    def scalaFilesIn(p: Path): List[Path] =
      filesIn(p).filter(_.toFile.ext == "scala")

    def markdownFilesInFile(f: File) =
      f.listFiles().filter(_.ext == "md")
  end FileIOBullshit

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
      "dev.zio"     %% "zio-process" % "0.7.1",
      "dev.zio" %% "zio-direct" % "1.0.0-RC7"
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
          "dev.zio" %% "zio-logging" % "2.1.12",
          "dev.zio" %% "zio-streams" %
            zioVersion,
          "dev.zio"   %% "zio-test" % zioVersion,
          "dev.zio"   %% "zio-test-sbt" %
            zioVersion % Test,
          "dev.zio"   %% "zio-prelude" %
            "1.0.0-RC16"
        ),
      scalaVersion := "3.2.2",
      scalacOptions -= "-explain-types",
      scalacOptions -= "-explain"
    )
end BuildTooling
