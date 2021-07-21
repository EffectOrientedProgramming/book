package zioBasics

import fakeEnvironmentInstances.FakeConsole

import java.io
import zio._
import zio.console._

import java.io.IOException

object Chaining extends zio.App {

  //The ZIO data type supports both flatMap and map.
  //This makes chaining multiple ZIOs within a for-comprehension much easier

  def chain: ZIO[Console, IOException, Unit] =
    for
      _ <- putStrLn(
        "Input a word: "
      ) //flatMap putStrLn ZIO into the wildcard char
      word <-
        getStrLn //flatmap getStrLn ZIO into word
      _ <- putStrLn(
        s"${word} is a nice word! Good choice!"
      )
    yield ()

  def run(args: List[String]) =
    chain
      .provideLayer(
        ZLayer.succeed(FakeConsole.word)
      )
      .exitCode
}
