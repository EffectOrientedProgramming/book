package testcontainers

import zio.Has
import org.testcontainers.containers.{
  Network,
  PostgreSQLContainer
}
import zio.ZServiceBuilder

object PostgresDummy

object PostgresContainer:
  def construct(initScipt: String): ZServiceBuilder[Has[
    Network
  ], Nothing, Has[PostgresContainerJ]] =
    for
      network <-
        ZServiceBuilder.service[Network].map(_.get)
      safePostgres =
        PostgresContainerJ
          .apply(initScipt, network)
          .nn
      res <-
        GenericInteractionsZ
          .manage(safePostgres, "postgres")
          .toServiceBuilder
    yield res
