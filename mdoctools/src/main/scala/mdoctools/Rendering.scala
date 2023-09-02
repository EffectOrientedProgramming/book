package mdoctools

object Rendering {
  def renderThrowableDefect(
                             defect: Throwable,
                             topLineLength: Int,
                           ) = {
    val msg = defect.toString
    val extractedMessage =
      if (msg != null && msg.nonEmpty)
        if (msg.contains("$"))
          msg
            .split("\\$")
            .last
            .replace(")", "")
        else
          msg
      else
        ""
    if (
      extractedMessage.length > topLineLength
    )
      extractedMessage.take(topLineLength)
    else
      extractedMessage

  }
  def renderThrowable(
                       error: Throwable,
                       columnWidth: Int,
                     ): String =
    error
      .toString
      .split("\n")
      .map(line =>
        if (line.length > columnWidth)
          throw new Exception(
            "Need to handle stacktrace line: " +
              line
          )
        else
          line
      )
      .mkString("\n")

  def renderError[E](error: E, topLineLength: Int): String =
    val extractedMessage = error.toString
    if (
      extractedMessage.length > topLineLength
    )
      extractedMessage.take(topLineLength)
    else
      extractedMessage



}
