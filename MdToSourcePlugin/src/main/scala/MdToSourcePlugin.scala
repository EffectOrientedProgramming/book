import sbt.Keys._
import sbt.{Def, _}
import sbt.plugins.JvmPlugin

import java.io.File

object MdToSourcePlugin extends AutoPlugin {
  override def requires = JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val MdToSource = config("MdToSource")
    lazy val mdDirectory = SettingKey[File]("mdDirectory", "MD Source Dir")
    lazy val runAll = TaskKey[Unit]("runAll")
  }

  import autoImport._


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

}
