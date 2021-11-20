package testcontainers

import org.testcontainers.containers.Network
import zio.{ZIO, ZManaged}
import zio.ZLayer

object Layers:
  lazy val networkLayer
      : ZLayer[Any, Nothing, Network] =
    ZManaged
      .acquireReleaseWith(
        ZIO.debug("Creating network") *>
          ZIO.succeed(Network.newNetwork().nn)
      )((n: Network) =>
        ZIO.attempt(n.close()).orDie *>
          ZIO.debug("Closing network")
      )
      .toLayer
