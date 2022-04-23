package Parallelism

import java.io.IOException
import zio.durationInt
import zio.{
  Fiber,
  IO,
  Runtime,
  UIO,
  ZIO,
  ZIOAppDefault,
  ZLayer
}

import scala.concurrent.Await

object JustSleep extends ZIOAppDefault:

  override def run =
    ZIO.collectAllPar(
      (1 to 10000).map(_ => ZIO.sleep(1.seconds))
    ) *>
      ZIO.debug(
        "Finished far sooner than 10,000 seconds"
      )

@main
def ToFuture() =
  Await.result(
    Runtime
      .default
      .unsafeRunToFuture(ZIO.sleep(1.seconds)),
    scala.concurrent.duration.Duration.Inf
  )
