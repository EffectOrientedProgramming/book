import sbt.IO

import java.io.File
import java.nio.file.Files
import sbt._
import Keys._
import java.io.File
import java.nio.file.{Files, Path, Paths}

case class FencedCode(content: String)

case class ProseFile(p: Path) {
  val cleanName = {
    val fileNameRaw = p.toFile.getName.toLowerCase.stripSuffix(".md")
    if (fileNameRaw.contains("_"))
      fileNameRaw.dropWhile(_ != '_').drop(1)
    else fileNameRaw
  }

}


object BuildTooling {
  def appendExperimentsToEndOfBookInNewChapter(packagedName: String, manuscript: File, experiments: List[FencedCode]): Unit = {
    val packageMarkdownFileName = packagedName.replaceAllLiterally("/", "-") + ".md"

    val nf = manuscript / packageMarkdownFileName
    println("Adding standalone example to end of book: " + nf.toString)

    nf.getParentFile.mkdirs()

    val md =
      s"""## ${packageMarkdownFileName.stripSuffix(".md")}
         |
         | ${experiments.map(_.content).mkString}
         |""".stripMargin


    Files.write(nf.toPath, md.getBytes)


    def addChapterToLeanpubIndex(chapterName: String): Unit =
      IO.append(manuscript / "Book.txt", chapterName + "\n")

    addChapterToLeanpubIndex(packageMarkdownFileName)
  }

  def appendExperimentsToMatchingProseFile(proseFile: ProseFile, experiments: List[FencedCode]) = {
    println("Should append to existing file for: " + proseFile)
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
    IO.append(proseFile.p.toFile, chapterExperiments)
  }


}

