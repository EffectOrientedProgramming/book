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

object MdocHelperSpec
    extends DefaultRunnableSpec:

  class PostgresContainer()
      extends PostgreSQLContainer[
        PostgresContainer
      ]("postgres:13.1")

  val network = Network.newNetwork()

  val postgresContainer =
    new PostgresContainer()
      .nn
      .withInitScript("init.sql")
      .nn
      .withNetwork(network)
      .nn
      .withNetworkAliases("postgres")
      .nn

  postgresContainer.start()

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test(
        "Intercept and format MatchError from unhandled RuntimeException"
      ) {
        for
          _ <-
            mdoc.wrapUnsafeZIO(
              ZIO.succeed(
                throw new MatchError(
                  MdocSession.App.GpsException()
                )
              )
            )
          output <- TestConsole.output
        yield assert(output)(
          equalTo(
            Vector(
              "Defect: class scala.MatchError\n",
              "        GpsException\n"
            )
          )
        )
      }
    )
end MdocHelperSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
