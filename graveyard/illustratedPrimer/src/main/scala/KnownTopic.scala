object KnownTopic:
  val topics =
    Set("concurrency", "time", "environment")

  // TODO Handle symbols
  def recognize(
      content: String
  ): List[ParagraphPiece] =
    val words = content.split(" ")
    words
      .map { word =>
        if topics.contains(word.toLowerCase) then
          ParagraphPiece.KnownTopic(word)
        else
          ParagraphPiece.Text(word)
      }
      .toList
