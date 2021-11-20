package zioBasics

import fakeEnvironmentInstances.FakeConsole

import java.io
import zio._
import zio.Console._

import java.io.IOException

object Chaining extends zio.App:

  // The ZIO data type supports both flatMap and
  // map.
  // This makes chaining multiple ZIOs within a
  // for-comprehension much easier

  def chain
      : ZIO[Console, IOException, Unit] =
    for
      _ <-
        printLine(
          "Input a word: "
        ) // flatMap println ZIO into the wildcard char
      word <-
        readLine // flatmap readLine ZIO into word
      _ <-
        printLine(
          s"${word} is a nice word! Good choice!"
        )
    yield ()

  def run(args: List[String]) =
    chain
      .provide(
        ZLayer.succeed(FakeConsole.word)
      )
      .exitCode
end Chaining
