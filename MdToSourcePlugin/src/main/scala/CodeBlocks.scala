sealed trait BookPiece
case class Prose(content: Seq[String]) extends BookPiece

case class BookLocation(fileName: String, startingLine: Int) {
  val clickableLink = s"$fileName:$startingLine"
}

case class CodeBlock(
    language: String,
    content: Seq[String],
    location: BookLocation,
    output: Option[Seq[String]] = None
) extends BookPiece

case class ValidatedCodeBlock(
    language: String,
    content: Seq[String],
    output: Option[Seq[String]] = None
)
case class NamedCodeBlock(fileName: String, codeBlock: ValidatedCodeBlock)

object NamedCodeBlock {

  def apply(fileName: String, codeBlock: CodeBlock): NamedCodeBlock = {
    val packagedContent =
      if (
        !codeBlock.content.exists(_.contains("package")) &&
        !codeBlock.content.headOption.exists(_.contains("{{incomplete}}"))
      ) {
        ("package " + fileName
          .replace("/", ".")
          .replace(".scala", "")
          .toLowerCase()) +: codeBlock.content
      } else {
        codeBlock.content
      }

    NamedCodeBlock(fileName, ValidatedCodeBlock(fileName, packagedContent))
  }
}
