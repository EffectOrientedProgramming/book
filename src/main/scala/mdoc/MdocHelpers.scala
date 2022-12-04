package mdoc

import zio.Runtime.default.unsafe
import zio.{Console, Unsafe, ZIO}

// Consider crashing if output is unexpectedly long
def wrapUnsafeZIOReportError[E, A](
    z: => ZIO[Any, E, A]
): ZIO[Any, java.io.IOException, String] =
  val commentPrefix = "// "
  val columnWidth =
    49 -
      commentPrefix
        .length // TODO Pull from scalafmt config file
  val defectPrefix = "Error: "
  val topLineLength =
    columnWidth - defectPrefix.length
  z.map(
      _.toString // TODO Respect width limit
    )
    .catchAll { case error: E =>
      val extractedMessage = error.toString
      val formattedMsg =
        if (
          extractedMessage.length > topLineLength
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

end wrapUnsafeZIOReportError

def unsafeRunPrettyPrint[E, A](
    z: => ZIO[Any, E, A]
): String =
  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    unsafe
      .run(wrapUnsafeZIOReportError(z))
      .getOrThrowFiberFailure()
  }
