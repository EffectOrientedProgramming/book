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
      network <- ZLayer.service[Network]
      safePostgres = apply(initScipt, network.get[Network])
      res <- ZManaged.acquireReleaseWith(
        ZIO.debug("Creating network") *> ZIO.succeed(safePostgres.start) *> ZIO.succeed(safePostgres)
      )((n: PostgresContainer) =>
        ZIO.attempt(n.close()).orDie *>
          ZIO.debug("Closing postgres")
      ).toLayer
    yield 
      res
end PostgresContainer
