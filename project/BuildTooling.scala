import sbt.IO
import sbt.*

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

  import java.nio.file.Path

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
           |[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/${
          proseFile
            .p
            .getFileName
            .toString
        })
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
}
