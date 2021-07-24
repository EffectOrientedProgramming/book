package Parallelism

import java.io.IOException
import zio.console.{getStrLn, putStrLn, Console}
import zio.duration.*
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
    ZIO
      .collectAllPar(
        (1 to 10000)
          .map(_ => ZIO.sleep(1.seconds))
      )
      .exitCode
end JustSleep

@main
def ToFuture() =
  Await.result(
    Runtime
      .default
      .unsafeRunToFuture(ZIO.sleep(1.seconds)),
    scala.concurrent.duration.Duration.Inf
  )
