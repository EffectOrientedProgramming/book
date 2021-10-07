package testcontainers

import zio.{Has, ZLayer}
import org.testcontainers.containers.{
  Network,
  PostgreSQLContainer
}

object PostgresDummy

object PostgresContainer:
  def construct(initScipt: String): ZLayer[Has[
    Network
  ], Nothing, Has[PostgresContainerJ]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      safePostgres =  PostgresContainerJ.apply(initScipt, network).nn
      res <-
        GenericInteractionsZ
          .manage(safePostgres, "postgres")
          .toLayer
    yield res
end PostgresContainer