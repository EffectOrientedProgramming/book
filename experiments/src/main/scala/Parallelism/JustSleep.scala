package Parallelism

import java.io.IOException
import zio.{
  Fiber,
  IO,
  Runtime,
  UIO,
  Unsafe,
  ZIO,
  ZIOAppDefault,
  ZLayer,
  durationInt
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
    Unsafe.unsafe { (_: Unsafe) =>
      zio
        .Runtime
        .default
        .unsafe
        .runToFuture(ZIO.sleep(1.seconds))
//        .getOrThrowFiberFailure()
    },
    scala.concurrent.duration.Duration.Inf
  )
