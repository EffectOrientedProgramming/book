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
  Network
}

import io.getquill._
import mdoc.QuillLocal.AppPostgresContext

case class Person(
    firstName: String,
    lastName: String,
    age: Int
)


object ManagedTestInstances:
  lazy val networkLayer
      : ZLayer[Any, Nothing, Has[Network]] =
    ZManaged
      .acquireReleaseWith(
        ZIO.debug("Creating network") *>
          ZIO.succeed(Network.newNetwork().nn)
      )((n: Network) =>
        ZIO.attempt(n.close()).orDie *>
          ZIO.debug("Closing network")
      )
      .toLayer

// TODO Figure out fi
// TESTCONTAINERS_RYUK_DISABLED=true is a
// band-aid that's avoiding the real problem with
// test cleanup

object TestContainersSpec
    extends DefaultRunnableSpec:

  import zio.durationInt

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test("With managed layer") {
        // TODO
        val logic =
          for
            people <- QuillLocal.quillQuery
          yield assert(people)(
            equalTo(
              List(Person("Joe", "Dimagio", 143))
            )
          )

        logic.provideSomeLayer[ZTestEnv & ZEnv](
          ManagedTestInstances.networkLayer >>>
            (PostgresContainer
              .construct("init.sql") >>>
            QuillLocal.quillPostgresContext) ++

              KafkaContainerLocal
                .construct()
        )
      }
    )
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
