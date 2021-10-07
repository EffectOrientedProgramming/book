package testcontainers

import zio.{Has, ZLayer}
import org.testcontainers.containers.{
  Network,
  PostgreSQLContainer
}

class Shit()

/* class PostgresContainerZ() extends
 *
 * object PostgresContainerZ:
 * def apply( initScript: String, network:
 * Network ): PostgresContainerZ =
 * new PostgresContainerZ() .nn
 * .withInitScript(initScript) .nn
 * .withNetwork(network) .nn
 * .withNetworkAliases("postgres") .nn
 *
 * def construct(initScipt: String): ZLayer[Has[
 * Network ], Nothing, Has[PostgresContainerZ]] =
 * for network <-
 * ZLayer.service[Network].map(_.get)
 * safePostgres = apply(initScipt, network) res
 * <- GenericInteractionsZ .manage(safePostgres,
 * "postgres") .toLayer yield res end
 * PostgresContainerZ */

case class PostgresContainerNew()
// extends PostgreSQLContainer[
//   PostgresContainerNew
// ]("postgres:13.1")
