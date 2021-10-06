package testcontainers

import zio.*
import zio.Console.*
import java.io.IOException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network,
  PostgreSQLContainer
}

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
        GenericInteractions
          .manage(safePostgres, "postgres")
          .toLayer
    yield res
end PostgresContainer
