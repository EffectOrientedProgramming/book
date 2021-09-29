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
}

object ManagedTestInstances:
  val network = ZManaged.acquireReleaseWith(ZIO.succeed(Network.newNetwork().nn))(n => ZIO.succeed(n.close)).useNow

object TestContainersSpec
    extends DefaultRunnableSpec:

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test(
        "Intercept and format MatchError from unhandled RuntimeException"
      ) {
        for
          network <- ManagedTestInstances.network
          safePostgres <- PostgresContainer.construct("init.sql")
            .provideSomeLayer(ZLayer.succeed(network))
          _ <- ZIO.succeed(safePostgres.start)
          _ <-
            mdoc.wrapUnsafeZIO(
              ZIO.succeed(
                throw new MatchError(
                  MdocSession.App.GpsException()
                )
              )
            )
          output <- TestConsole.output
          _ <- printLine("With managed Network 1")
          _ <- ZIO.succeed(safePostgres.close)
          _ <- ZIO.succeed(network.close)
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
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
