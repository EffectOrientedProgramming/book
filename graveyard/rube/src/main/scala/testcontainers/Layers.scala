package testcontainers

import org.testcontainers.containers.Network
import zio.{Scope, ZIO, ZLayer}

object Layers:
  lazy val networkLayer
      : ZLayer[Scope, Nothing, Network] =
    ZLayer.fromZIO(
      ZIO.acquireRelease {
        ZIO.debug("Creating network") *>
          ZIO.succeed(Network.newNetwork())
      } {
        n =>
          ZIO.attempt(n.close()).orDie *>
            ZIO.debug("Closing network")
      }
    )
