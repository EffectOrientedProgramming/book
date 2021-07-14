/* What do we need for the full pandoc process?
  1. Get all .md files
  2. concatenate them together
 */
import ammonite.ops._
import pprint.PPrinter
//val listed = ls! (pwd / up )
val eopDir = pwd / up / up / up / up
val markdownDir = eopDir / "Chapters"
val markdownFiles = ls! markdownDir
val allContents = markdownFiles.map(read)
write(eopDir/"target"/"fullBook.md", allContents.mkString("\n"))
PPrinter.BlackWhite.pprintln(allContents)
//pprint.pprintln(allContents)
//val contents = read(resource/'test/'ammonite/'ops/'folder/"file.txt")
