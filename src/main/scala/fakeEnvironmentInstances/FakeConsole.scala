package fakeEnvironmentInstances

import zio._
import zio.console.Console.Service
import zio.console._

import java.io.IOException

object FakeConsole:
  import zio.console.Console.Service

  val name: Service = singleInputConsole("(default name)")
  val word: Service = singleInputConsole("Banana")
  val number: Service = singleInputConsole("1")

  def singleInputConsole(hardcodedInput: String) = new Service:
    val getStrLn: IO[IOException, String] = ZIO.succeed(hardcodedInput)

    def putStr(line: String): zio.IO[java.io.IOException, Unit] = ???
    def putStrErr(line: String): zio.IO[java.io.IOException, Unit] = ???
    def putStrLnErr(line: String): zio.IO[java.io.IOException, Unit] = ???

    def putStrLn(line: String): IO[IOException, Unit] =
      ZIO.succeed(println("Automated: " + line))
