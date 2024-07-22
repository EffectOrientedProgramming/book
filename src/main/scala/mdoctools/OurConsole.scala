package mdoctools

import java.io.{IOException, PrintStream}
import zio.*


// This is an insane "solution" to mdoc gobbling ZIO console output when run more than once in watch mode
// This takes an optional out PrintStream that enables capturing the mdoc override of the out PrintStream and using it
class OurConsole(
    out: Option[PrintStream] =
      None
) extends Console:
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
    ZIO.succeed:
      out.fold(scala.Console.println(line)):
        myOut =>
          scala
            .Console
            .withOut(myOut):
              scala.Console.println(line)

  override def printLineError(line: => Any)(
      implicit trace: Trace
  ): IO[IOException, Unit] =
    ???

  override def readLine(implicit
      trace: Trace
  ): IO[IOException, String] =
    ???
end OurConsole
