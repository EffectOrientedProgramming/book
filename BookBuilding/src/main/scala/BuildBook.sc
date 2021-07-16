/* What do we need for the full pandoc process?
  1. Get all .md files
  2. concatenate them together
 */
import ammonite.ops._
import pprint.PPrinter
//val listed = ls! (pwd / up )
val eopDir = pwd / up / up / up / up
val markdownDir = eopDir / "manuscript"
val bookTxt = markdownDir / "Book.txt"
val markdownFiles = ls! markdownDir
val allContents = markdownFiles.map(read)
write.over(eopDir / "target" / "fullBook.md", allContents.mkString("\n"))

rm! bookTxt
write(bookTxt, "")

markdownFiles.foreach {
  mdFile =>
    write.append(bookTxt, mdFile.last + "\n")
}

// PPrinter.BlackWhite.pprintln(allContents)
