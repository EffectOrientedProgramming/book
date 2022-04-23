package testcontainers

import org.testcontainers.containers.{
  Network,
  PostgreSQLContainer
}
import zio.{ZEnvironment, ZIO, ZLayer}

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

          ZLayer.fromZIO(
            ZIO.scoped {
              GenericInteractionsZ
                .manage(safePostgres, "postgres")
            }
          )
      }
end PostgresContainer
