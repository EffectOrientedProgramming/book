import sbt.IO
import sbt.*
import Keys.*

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
    import mdoc.internal.cli.Settings
    import mdoc.internal.markdown.MarkdownFile
    import mdoc.parser.CodeFence

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

          val contents =
            header ++ indentedBlocks

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
    ): Unit = {
      println("attaching edit link")
      val editLink =
        s"""
           |
           |[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/${proseFile
            .p
            .getFileName
            .toString})
           |""".stripMargin
      val lines =
        IO.readLines(proseFile.p.toFile)
      val linesWithEditLink =
        lines.head :: (editLink :: lines.tail)
      IO.writeLines(
        proseFile.p.toFile,
        linesWithEditLink,
        append =
          false
      )
    }

    proseFiles.foreach(attachEditLink)
//    attachEditLink(value)

  }

  // TODO Make a Versions object?
  val zioVersion =
    "2.0.21"

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
          start =
            "-Yimports:",
          sep =
            ",",
          end =
            ""
        ),
      libraryDependencies ++=
        Seq(
          "dev.zio" %% "zio"       % zioVersion,
          "dev.zio" %% "zio-cache" % "0.2.3",
          "dev.zio" %% "zio-config" % "4.0.1",
          "dev.zio" %% "zio-config-magnolia" % "4.0.1",
          "dev.zio" %% "zio-config-typesafe" % "4.0.1",
          "dev.zio"     %%
            "zio-direct" % "1.0.0-RC7" excludeAll
            (
              "com.geirsson",
              "metaconfig-typesafe-config"
            ) excludeAll
            ("com.geirsson", "metaconfig-core"),
          "dev.zio"   %% "zio-test" % zioVersion,
          "dev.zio"   %% "zio-test-sbt" %
            zioVersion % Test,
          "nl.vroste" %% "rezilience"   % "0.9.4"
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
