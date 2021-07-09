object ContentRules {

  def flattenBookPieceBackToString(bookPiece: BookPiece): String =
    bookPiece match {
      case Prose(content) => content.mkString("\n")
      case CodeBlock(
            language,
            content,
            BookLocation(fileName, startingLine),
            output
          ) =>
        if (content.contains(".scala")) // TODO Kludgy
          ("```scala" +: content :+ "```").mkString("\n")
        else
          ("```" +: content :+ "```").mkString("\n")

    }

  def digestContentsAndApplyUpdatesInPlace(
      contents: String,
      fileName: String
  ): String = {
    val realResultsThatShouldEventuallyBeReturn =
      FenceFinder
        .categorizedContents(contents, fileName)
        .map(flattenBookPieceBackToString)
        .mkString("\n")
    contents
  }

}
