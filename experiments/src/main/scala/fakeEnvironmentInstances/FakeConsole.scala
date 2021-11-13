package fakeEnvironmentInstances

import zio._
import zio.Console
import zio.Console._

import java.io.IOException

object FakeConsole:

  val name: Console = single("(default name)")

  val word: Console   = single("Banana")
  val number: Console = single("1")

  def single(hardcodedInput: String) =
    new Console:
      def print(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(print("Hard-coded: " + line))
      def printError(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] = ???
      def printLine(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(
          println("Hard-coded: " + line)
        )
      def printLineError(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] = ???
      def readLine(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, String] =
        ZIO.succeed(hardcodedInput)

  def withInput(
      hardcodedInput: String*
  ): ZIO[Any, Nothing, Console] =
    for
      inputVariable <-
        Ref.make(hardcodedInput.toSeq)
    yield inputConsole(inputVariable)

  private def inputConsole(
      hardcodedInput: Ref[Seq[String]]
  ) =
    new Console:
      def print(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] =
        IO.succeed(print(line))

      def printError(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] = ???

      def printLine(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] =
        ZIO
          .succeed(println("Automated: " + line))

      def printLineError(line: => Any)(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, Unit] = ???

      def readLine(implicit
          trace: zio.ZTraceElement
      ): zio.IO[java.io.IOException, String] =
        for
          curInput <- hardcodedInput.get
          _ <- hardcodedInput.set(curInput.tail)
        yield curInput.head

end FakeConsole
