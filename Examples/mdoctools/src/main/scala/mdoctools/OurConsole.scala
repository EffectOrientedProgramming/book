package mdoctools

import java.io.IOException

// This is an insane "solution" to mdoc gobbling ZIO console output when run more than once in watch mode
object OurConsole extends Console:
  override def print(line: => Any)(implicit
      trace: Trace
  ): IO[IOException, Unit] =
    ???

  override def printError(line: => Any)(implicit
      trace: Trace
  ): IO[IOException, Unit] =
    ???

  override def printLine(line: => Any)(implicit
      trace: Trace
  ): IO[IOException, Unit] =
    ZIO.succeed(println(line))

  override def printLineError(line: => Any)(
      implicit trace: Trace
  ): IO[IOException, Unit] =
    ???

  override def readLine(implicit
      trace: Trace
  ): IO[IOException, String] =
    ???
end OurConsole
