package mdoc

import zio.*
import zio.Console.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*
import java.io.IOException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network,
  PostgreSQLContainer
}
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class PostgresContainer()
    extends PostgreSQLContainer[
      PostgresContainer
    ]("postgres:13.1")

object PostgresContainer:
  def apply(
      initScript: String,
      network: Network
  ): PostgresContainer =
    new PostgresContainer()
      .nn
      .withInitScript(initScript)
      .nn
      .withNetwork(network)
      .nn
      .withNetworkAliases("postgres")
      .nn

  def construct(initScipt: String): ZLayer[Has[
    Network
  ], Nothing, Has[PostgresContainer]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      safePostgres = apply(initScipt, network)
      res <-
        ZManaged
          .acquireReleaseWith(
            ZIO.debug("Creating postgres") *>
              ZIO.succeed(safePostgres.start) *>
              ZIO.succeed(safePostgres)
          )((n: PostgresContainer) =>
            ZIO.attempt(n.close()).orDie *>
              ZIO.debug("Closing postgres")
          )
          .toLayer
    yield res
end PostgresContainer

object KafkaStuff:
  import org.testcontainers.containers.KafkaContainer


object KafkaContainerLocal:
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
            ZIO.debug("Creating kafka") *>
              ZIO.succeed(safePostgres.start) *>
              ZIO.succeed(safePostgres)
          )((n: KafkaContainer) =>
            ZIO.attempt(n.close()).orDie *>
              ZIO.debug("Closing kafka")
          )
          .toLayer
    yield res
end KafkaContainerLocal