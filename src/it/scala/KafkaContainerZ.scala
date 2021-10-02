package mdoc

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  Network,
}
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

object GenericInteractions:
  def interactWith[T <: GenericContainer[T]](c: T, containerType: String) =
    ZIO.blocking(ZIO.succeed(c.start)) *> ZIO.debug(s"Finished blocking during $containerType container creation")

  def manage[T <: GenericContainer[T]](c: T, containerType: String) =
    ZManaged
      .acquireReleaseWith(
        ZIO.debug(s"Creating $containerType") *>
          interactWith(c, containerType) *>
          ZIO.succeed(c)
      )((n: T) =>
        ZIO.attempt(n.close()).orDie *>
          ZIO.debug(s"Closing $containerType")
      )

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
      container = apply(network)
      res <-
          GenericInteractions.manage(container, "kafka")
          .toLayer
    yield res
end KafkaContainerZ 
