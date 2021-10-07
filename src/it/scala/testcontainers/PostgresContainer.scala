package testcontainers

import zio.{Has, ZLayer}
import org.testcontainers.containers.{
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
        GenericInteractionsZ
          .manage(safePostgres, "postgres")
          .toLayer
    yield res
end PostgresContainer
