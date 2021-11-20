package testcontainers

import org.testcontainers.containers.{
  Network,
  PostgreSQLContainer
}
import zio.{ZEnvironment, ZLayer}

object PostgresDummy

object PostgresContainer:
  def construct(initScipt: String): ZLayer[
    Network,
    Nothing,
    PostgresContainerJ
  ] =
    ZLayer
      .service[Network]
      .flatMap {
        (network: ZEnvironment[Network]) =>
          val safePostgres =
            PostgresContainerJ
              .apply(initScipt, network.get)
              .nn
          GenericInteractionsZ
            .manage(safePostgres, "postgres")
            .toLayer
      }
