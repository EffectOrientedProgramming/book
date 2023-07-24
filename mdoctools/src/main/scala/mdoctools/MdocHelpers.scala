package mdoctools

import zio.Runtime.default.unsafe

object Stuff:
  object WithALongName:
    object ThatWillComplicate:
      def run =
        throw new Exception(
          "Boom stoinky kablooey pow pow pow"
        )

val commentPrefix = "// "
val columnWidth =
  49 -
    commentPrefix
      .length // TODO Pull from scalafmt config file

private def renderThrowable(
    error: Throwable
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

// Consider crashing if output is unexpectedly long
def wrapUnsafeZIOReportError[E, A](
    z: => ZIO[Any, E, A]
): ZIO[Any, java.io.IOException, String] =
  val defectPrefix = "Error: "
  val topLineLength =
    columnWidth - defectPrefix.length
  z.map(result => result.toString)
    .catchAll {
      case error: Throwable =>
        ZIO.succeed(renderThrowable(error))
      case error: E =>
        val extractedMessage = error.toString
        val formattedMsg =
          if (
            extractedMessage.length >
              topLineLength
          )
            extractedMessage.take(topLineLength)
          else
            extractedMessage

        ZIO.succeed(formattedMsg)
    }
    .catchAllDefect(defect =>
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
      val formattedMsg =
        if (
          extractedMessage.length > topLineLength
        )
          extractedMessage.take(topLineLength)
        else
          extractedMessage

      ZIO.succeed("Defect: " + formattedMsg)
    )
    .map { result =>
      result
        .split("\n")
        .map(line =>
          if (line.length > columnWidth)
            throw new Exception(
              "Line too long: \n" + line
            )
          else
            line
        )
        .mkString("\n")
      // TODO Respect width limit
    }
    .tap(finalValueToRender =>
      ZIO.debug(finalValueToRender)
    )

end wrapUnsafeZIOReportError

def runDemoValue[E, A](
    z: => ZIO[Any, E, A]
): String =
  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    unsafe
      .run(wrapUnsafeZIOReportError(z))
      .getOrThrowFiberFailure()
  }

def runDemo[E, A](z: => ZIO[Any, E, A]): Unit =
  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    unsafe
      .run(wrapUnsafeZIOReportError(z))
      .getOrThrowFiberFailure()
    //      .getOrThrowFiberFailure()
  }

// Should be copied!
