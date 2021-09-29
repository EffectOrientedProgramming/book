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
  def apply(initScript: String, network: Network): PostgresContainer =
    new PostgresContainer()
      .nn
      .withInitScript(initScript)
      .nn
      .withNetwork(network)
      .nn
      .withNetworkAliases("postgres")
      .nn

  def construct(
  initScipt: String
  ): ZIO[Has[Network], Nothing, PostgresContainer] =
    for {
      network <- ZIO.service[Network]
    } yield  apply(initScipt, network)