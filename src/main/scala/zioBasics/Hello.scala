package zioBasics

import java.io.IOException
import zio.console.{
  Console,
  getStrLn,
  putStrLn
}
import fakeEnvironmentInstances.FakeConsole
import zio.console.Console.Service
import zio.{IO, Runtime, ZIO, ZLayer}

@main def hello =
  val a = println("1")
  val s: ZIO[Any, Nothing, String] =
    ZIO.succeed { println("2"); "asdf"; }
  val h: ZIO[Console, IOException, Unit] =
    s.flatMap { ss =>
      println("3"); putStrLn(ss);
    }
  println("4")
  Runtime.default.unsafeRunSync(h)

@main def scheduling =
  import zio.Schedule
  import zio.duration.*

  val scheduledCode =
    for
      _ <- putStrLn("Looping. Give me a word")
      input <- getStrLn
      _ <-
        if (input != "boom")
          putStrLn("Fine. Keep going.")
        else
          ZIO.fail("errrgggg")
    yield ()

  Runtime.default.unsafeRunSync(
    scheduledCode
      .provideLayer(
        ZLayer.succeed(FakeConsole.word)
      )
      .repeat(
        Schedule.recurs(4) && Schedule.spaced(
          3.seconds
        )
      )
  )

@main def ValPassing(): Unit =

  val input: ZIO[
    zio.console.Console,
    IOException,
    String
  ] =
    for
      _ <- putStrLn("What is your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, ${name}")
    yield name

  def process(input: String): Boolean =
    if (input == "Bob")
      true
    else
      false

  val b: ZIO[Console, IOException, Boolean] =
    input.map(i => process(i))

  println(
    Runtime.default.unsafeRunSync(
      b.provideLayer(
        ZLayer.succeed(FakeConsole.name)
      ).exitCode
    )
  )
