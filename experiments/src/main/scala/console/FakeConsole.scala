package console

import zio._
import zio.Console
import zio.Console._
import zio.direct._

import java.io.IOException

object FakeConsole:

  val name: Console = single("(default name)")

  val word: Console   = single("Banana")
  val number: Console = single("1")

  def single(hardcodedInput: String) =
    new Console:
      def print(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(print("Hard-coded: " + line))
      def printError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???
      def printLine(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(
          println("Hard-coded: " + line)
        )
      def printLineError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???
      def readLine(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, String] =
        ZIO.succeed(hardcodedInput)

  def withInput(
      hardcodedInput: String*
  ): ZIO[Any, Nothing, Console] =
    defer {
      val inputVariable =
        Ref.make(hardcodedInput.toSeq).run
      inputConsole(inputVariable)
    }

  private def inputConsole(
      hardcodedInput: Ref[Seq[String]]
  ) =
    new Console:
      def print(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(print(line))

      def printError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???

      def printLine(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] =
        ZIO
          .succeed(println("Automated: " + line))

      def printLineError(line: => Any)(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, Unit] = ???

      def readLine(implicit
          trace: zio.Trace
      ): zio.IO[java.io.IOException, String] =
        defer {
          val curInput = hardcodedInput.get.run
          hardcodedInput.set(curInput.tail).run
          curInput.head
        }

end FakeConsole
