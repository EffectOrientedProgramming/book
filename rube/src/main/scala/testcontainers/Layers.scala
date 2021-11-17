package testcontainers

import org.testcontainers.containers.Network
import zio.{Has, ZIO, ZManaged}
import zio.ZServiceBuilder

object Layers:
  lazy val networkLayer
      : ZServiceBuilder[Any, Nothing, Has[Network]] =
    ZManaged
      .acquireReleaseWith(
        ZIO.debug("Creating network") *>
          ZIO.succeed(Network.newNetwork().nn)
      )((n: Network) =>
        ZIO.attempt(n.close()).orDie *>
          ZIO.debug("Closing network")
      )
      .toServiceBuilder
