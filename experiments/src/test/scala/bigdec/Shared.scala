package bigdec

import zio.*

import java.util.concurrent.atomic.AtomicInteger

object Shared {
  private val layerBuildCount = new AtomicInteger(0)
  val layer: ZLayer[Scope, Nothing, Int] = ZLayer.scoped[Scope] {
    for {
      scope <- ZIO.service[Scope]
      _ <- scope.addFinalizer{
        ZIO.succeed(layerBuildCount.get()).debug("Final startup count")
      }
    } yield {
      println("Initializing expensive layer!")
      layerBuildCount.incrementAndGet()
    }
  }

}