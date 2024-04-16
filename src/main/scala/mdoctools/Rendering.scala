package mdoctools

object LineLength:
  val commentPrefix =
    "// "
  val columnWidth =
    49 -
      commentPrefix
        .length // TODO Pull from scalafmt config file

  val defectPrefix =
    "Error: "
  val topLineLength =
    columnWidth - defectPrefix.length

object Rendering:
  def renderEveryPossibleOutcomeZio[R, E, A](
      z: => ZIO[R, E, A]
  ): ZIO[R, E, String] =
    z.map(
        result => result.toString
      )
      .catchAll {
        case error: Throwable =>
          ZIO.succeed(
            Rendering.renderThrowable(error)
          )
        case error: E =>
          ZIO.succeed(
            Rendering.renderError(error)
          )
      }
      .catchAllDefect(
        defect =>
          ZIO.succeed(
            Rendering
              .renderThrowableDefect(defect)
          )
      )
      .map {
        result =>
          Rendering
            .lastBruteForceLineLengthPhase(
              result
            )
      }

  def renderThrowableDefect(defect: Throwable) =
    val msg =
      defect.toString
    val extractedMessage =
      if (msg != null && msg.nonEmpty)
        if (msg.contains("$"))
          msg.split("\\$").last.replace(")", "")
        else
          msg
      else
        ""
    "Defect: " +
      (if (
         extractedMessage.length >
           LineLength.topLineLength
       )
         extractedMessage
           .take(LineLength.topLineLength)
       else
         extractedMessage)
  end renderThrowableDefect

  def renderThrowable(error: Throwable): String =
    error
      .toString
      .split("\n")
      .map(
        line =>
          if (
            line.length > LineLength.columnWidth
          )
            throw new Exception(
              "Need to handle stacktrace line: " +
                line
            )
          else
            line
      )
      .mkString("\n")

  def renderError[E](error: E): String =
    val extractedMessage =
      error.toString
    if (
      extractedMessage.length >
        LineLength.topLineLength
    )
      extractedMessage
        .take(LineLength.topLineLength)
    else
      extractedMessage

  def lastBruteForceLineLengthPhase(
      result: String
  ) =
    result
      .split("\n")
      .map(
        line =>
          if (
            line.length > LineLength.columnWidth
          )
            println(
              "TODO Handle long line. \n" +
                "Truncating for now: \n" + line
            )
            line.take(LineLength.columnWidth)
          else
            line
      )
      .mkString("\n")
end Rendering
