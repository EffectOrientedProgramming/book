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
import org.testcontainers.containers.KafkaContainer

import mdoc._



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
        val logicWithAssertions =
          for {
            people <- ContainerScenarios.logic
          }
          yield assert(people.head)(
            equalTo(
              Person("Joe", "Dimagio", 143)
            )
          )
        import org.testcontainers.containers.MockServerContainer

        val careerServer: ZLayer[Has[Network], Throwable, Has[CareerHistoryService]] =
          CareerHistoryService.construct(
                        List(
                          RequestResponsePair(
                            "/person/Joe",
                            "Joe is a dynamic baseball player!"
                          ),
                          RequestResponsePair(
                            "/person/Shtep",
                            "Shtep has sold fizzy drinks for many years."
                          ),
                          RequestResponsePair(
                            "/person/Zeb",
                            "Zeb worked at a machine shop."
                          )
                        ),
                      )

        val locationServer: ZLayer[Has[Network], Throwable, Has[LocationService]] =
          LocationService.construct(
                        List(
                          RequestResponsePair(
                            "/location/Joe",
                            "USA"
                          ),
                          RequestResponsePair(
                            "/location/Shtep",
                            "Jordan"
                          ),
                          RequestResponsePair(
                            "/location/Zeb",
                            "Taiwan"
                          )
                        ),
                      )

        val layer: ZLayer[Any, Throwable, Has[Network] & Has[NetworkAwareness] & (Has[PostgresContainer] & Has[KafkaContainer]) & Has[AppPostgresContext] & Has[CareerHistoryService]] =
          ((ManagedTestInstances.networkLayer ++
            NetworkAwareness.live) >+>
            (PostgresContainer
              .construct("init.sql") ++
              KafkaContainerZ.construct())) >+>
            (QuillLocal.quillPostgresContext) ++
            (careerServer)

        logicWithAssertions.provideSomeLayer[ZTestEnv & ZEnv](
          layer
        )
      }
      // test("stream approach") {
      //   val logic = ???
      //   ???
      // }
    )
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
