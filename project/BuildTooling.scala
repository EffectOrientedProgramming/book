import sbt.IO
import sbt.*
import Keys.*
import mdoc.internal.cli.Settings

import java.io.File
import java.nio.file.Path

case class FencedCode(content: String)

case class ProseFile(p: Path) {
  val cleanName = {
    val fileNameRaw =
      p.toFile
        .getName
        .toLowerCase
        .stripSuffix(".md")
    if (fileNameRaw.contains("_"))
      fileNameRaw.dropWhile(_ != '_').drop(1)
    else
      fileNameRaw
  }

}

object BuildTooling {

  def appendExperimentsToEndOfBookInNewChapter(
      packagedName: String,
      manuscript: File,
      experiments: List[FencedCode]
  ): Unit = {
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
  }

  def appendExperimentsToMatchingProseFile(
      proseFile: ProseFile,
      experiments: List[FencedCode]
  ): Unit = {
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
  }

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
  lazy val examplesHelperDir =
    settingKey[File]("Examples Helper Dir")
  lazy val generateExamples =
    taskKey[Unit]("generate examples")

  val generateExamplesTask =
    Def.task {
      generateExamplesLogic(
        examplesDir.value,
        examplesHelperDir.value,
        mdDir.value
      )
    }

  private def generateExamplesLogic(
      examplesDirectory: File,
      examplesHelperDirectory: File,
      markdownDir: File
  ): Unit = {
    if (examplesDirectory.exists())
      FileIOBullshit
        .deleteAllScalaFilesRecursively(
          examplesDirectory
        )
    else
      examplesDirectory.mkdirs()

    // Examples/mdoctools/src/main/scala/mdoctools

    if (examplesHelperDirectory.exists())
      FileIOBullshit
        .deleteAllScalaFilesRecursively(
          examplesHelperDirectory
        )

    // *Very* weird hacky bit of code to get this
    // not making the last level of the path
    // the directory indclues "/scala" already,
    // but this is needed??
    examplesHelperDirectory./("scala").mkdirs()

    FileIOBullshit.copyFolder(
      Paths
        .get(".")
        .resolve("mdoctools")
        .resolve("src")
        .resolve("main")
        .resolve("scala"),
      examplesHelperDirectory.toPath
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
              ConsoleReporter.default,
              Settings.default(
                AbsolutePath.workingDirectory
              )
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

  }

  def generateBookTxtFromNumberedChapters(
      proseDirectory: File,
      leanPubDirectory: File
  ): Path = {
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
    ): Option[NumberedChapter] = {
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
    }

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
  }

  def produceLeanpubManuscript(
      leanPubDirectory: File
  ): Unit = {
    // Files that we have actually written
    // content for
    // They will be used as anchors for attaching
    // experiments
    val proseFiles: Seq[ProseFile] =
      FileIOBullshit
        .markdownFilesIn(leanPubDirectory.toPath)
        .sortBy(_.toFile.getName)
        .map(ProseFile)

    def attachEditLink(
        proseFile: ProseFile
    ): Unit =
      IO.append(
        proseFile.p.toFile,
        // TODO Verify this link
        s"""
           |
           |## Edit This Chapter
           |[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/${proseFile
            .p
            .getFileName
            .toString})
           |""".stripMargin
      )

    proseFiles.foreach(attachEditLink)
//    attachEditLink(value)

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

        proseFileOnSameTopic match {
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
    }

  }

  case class ExperimentFile(p: Path)

  private def fileFence(
      experimentFile: ExperimentFile
  ): FencedCode = {
    val file  = experimentFile.p.toFile
    val lines = IO.read(file)
    FencedCode(s"""
         |
         |### ${file.toString}
         |```scala
         |$lines
         |```
         |""".stripMargin)
  }

  def experimentLibrariesThatNeedToBeVettedForInclusionInBook =
    Seq(
      "org.apache.commons" % "commons-text" %
        "1.11.0",
      "io.github.scottweaver" %%
        "zio-2-0-testcontainers-postgresql" %
        "0.10.0",
      "io.github.scottweaver"        %%
        "zio-2-0-db-migration-aspect" % "0.10.0",
      "io.getquill" %% "quill-jdbc-zio" %
        "4.8.0",
      "io.getquill" %% "quill-zio"   % "4.8.0",
      "nl.vroste"   %% "rezilience"  % "0.9.4",
      "dev.zio"     %% "zio-process" % "0.7.2",
      "dev.zio" %% "zio-direct" % "1.0.0-RC7",
      "dev.zio" %% "zio-schema" % "0.4.15",
      "dev.zio" %% "zio-schema-json" % "0.4.15",
      "dev.zio" %% "zio-schema-derivation" %
        "0.4.15"
      // "org.scala-lang" % "scala-reflect" %
      // scalaVersion.value % "provided"
//    "dev.zio" %% "zio-direct-streams" % "1.0.0-RC7" // TODO Enable when on the ground :(
    )

  // TODO Make a Versions object?
  val zioVersion = "2.0.18"

  lazy val commonSettings =
    Seq(
      scalacOptions +=
        Seq(
          "java.lang",
          "scala",
          "scala.Predef",
          "zio",
          "zio.direct"
        ).mkString(
          start = "-Yimports:",
          sep = ",",
          end = ""
        ),
      libraryDependencies ++=
        Seq(
          "dev.zio" %% "zio"       % zioVersion,
          "dev.zio" %% "zio-cache" % "0.2.3",
          "dev.zio" %% "zio-concurrent" %
            zioVersion,
          "dev.zio"     %%
            "zio-direct" % "1.0.0-RC7" excludeAll
            (
              "com.geirsson",
              "metaconfig-typesafe-config"
            ) excludeAll
            (
              "com.geirsson",
              "metaconfig-core"
            ) excludeAll
            ("org.typelevel", "paiges-core"),
          "dev.zio" %% "zio-logging" % "2.1.15",
          "dev.zio" %% "zio-streams" %
            zioVersion,
          "dev.zio"   %% "zio-test" % zioVersion,
          "dev.zio"   %% "zio-test-sbt" %
            zioVersion % Test,
          "dev.zio"   %% "zio-prelude" %
            "1.0.0-RC21"
        ),
      scalaVersion := "3.3.1",
      scalacOptions -= "-explain-types",
      scalacOptions -= "-explain",
      fork := true,
      Compile / packageDoc / publishArtifact :=
        false,
      Compile / doc / sources := Seq.empty
    )

}
