package Parallelism

import java.io.IOException
import zio.Console.{getStrLn, putStrLn}
import zio.durationInt
import zio.{
  App,
  Fiber,
  IO,
  Runtime,
  UIO,
  ZIO,
  ZLayer
}

import scala.concurrent.Await

object JustSleep extends App:

  override def run(args: List[String]) =
    ZIO.collectAllPar(
      (1 to 10000).map(_ => ZIO.sleep(1.seconds))
    ) *>
      ZIO
        .debug(
          "Finished far sooner than 10,000 seconds"
        )
        .exitCode

@main
def ToFuture() =
  Await.result(
    Runtime
      .default
      .unsafeRunToFuture(ZIO.sleep(1.seconds)),
    scala.concurrent.duration.Duration.Inf
  )
