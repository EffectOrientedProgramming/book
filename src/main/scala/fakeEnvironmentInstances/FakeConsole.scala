package fakeEnvironmentInstances

import zio._
import zio.Console
import zio.Console._

import java.io.IOException

object FakeConsole:

  val name: Console =
    singleInputConsole("(default name)")

  val word: Console =
    singleInputConsole("Banana")
  val number: Console = singleInputConsole("1")

  def singleInputConsole(
      hardcodedInput: String
  ) =
    new Console:

      override val getStrLn
          : IO[IOException, String] =
        ZIO.succeed(hardcodedInput)

      override def putStr(
          line: String
      ): zio.IO[java.io.IOException, Unit] = ???

      override def putStrErr(
          line: String
      ): zio.IO[java.io.IOException, Unit] = ???

      override def putStrLnErr(
          line: String
      ): zio.IO[java.io.IOException, Unit] = ???

      override def putStrLn(
          line: String
      ): IO[IOException, Unit] =
        ZIO.succeed(
          println("Hard-coded: " + line)
        )

      override def print(
          line: Any
      ): zio.IO[java.io.IOException, Unit] = ???
      override def printError(
          line: Any
      ): zio.IO[java.io.IOException, Unit] = ???
      override def printLine(
          line: Any
      ): zio.IO[java.io.IOException, Unit] =
        ZIO.succeed(
          println("Hard-coded: " + line)
        )
      override def printLineError(
          line: Any
      ): zio.IO[java.io.IOException, Unit] = ???
      override def readLine
          : zio.IO[java.io.IOException, String] =
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

      def curInput(
          input: => Seq[String]
      ): Seq[String] = input

      override def getStrLn
          : IO[IOException, String] =
        for
          curInput <- hardcodedInput.get
          _ <- hardcodedInput.set(curInput.tail)
        yield curInput.head

      override def putStr(
          line: String
      ): zio.IO[java.io.IOException, Unit] = ???

      override def putStrErr(
          line: String
      ): zio.IO[java.io.IOException, Unit] = ???

      override def putStrLnErr(
          line: String
      ): zio.IO[java.io.IOException, Unit] = ???

      override def putStrLn(
          line: String
      ): IO[IOException, Unit] =
        ZIO
          .succeed(println("Automated: " + line))

      override def print(
          line: Any
      ): zio.IO[java.io.IOException, Unit] = ???
      override def printError(
          line: Any
      ): zio.IO[java.io.IOException, Unit] = ???
      override def printLine(
          line: Any
      ): zio.IO[java.io.IOException, Unit] =
        ZIO
          .succeed(println("Automated: " + line))
      override def printLineError(
          line: Any
      ): zio.IO[java.io.IOException, Unit] = ???
      override def readLine
          : zio.IO[java.io.IOException, String] =
        for
          curInput <- hardcodedInput.get
          _ <- hardcodedInput.set(curInput.tail)
        yield curInput.head

end FakeConsole
