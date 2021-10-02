package mdoc

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  Network,
}
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

object KafkaContainerZ:
  def apply(
      network: Network
  ): KafkaContainer =
    new KafkaContainer(
      DockerImageName
        .parse("confluentinc/cp-kafka:5.4.3")
        .nn
    ).nn

  def construct(): ZLayer[Has[
    Network
  ], Nothing, Has[KafkaContainer]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      safePostgres = apply(network)
      res <-
        ZManaged
          .acquireReleaseWith(
            ZIO.debug("Creating kafka!") *>
              ZIO.succeed(safePostgres.start) *>
              ZIO.succeed(safePostgres)
          )((n: KafkaContainer) =>
            ZIO.attempt(n.close()).orDie *>
              ZIO.debug("Closing kafka")
          )
          .toLayer
    yield res
end KafkaContainerZ 
