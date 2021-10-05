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
import org.testcontainers.containers.KafkaContainer

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

// lazy val networkAwareness = ???

trait NetworkAwareness:
  val localHostName: Task[String]

object NetworkAwareness:
  val localHostName: ZIO[Has[
    NetworkAwareness
  ], Throwable, String] =
    ZIO.serviceWith(_.localHostName)

  val live
      : Layer[Nothing, Has[NetworkAwareness]] =
    ZLayer.succeed(NetworkAwarenessLive)

object NetworkAwarenessLive
    extends NetworkAwareness:
  import java.net.InetAddress
  val localHostName =
    ZIO.attempt {
      InetAddress
        .getLocalHost()
        .nn
        .getHostName()
        .nn
    }

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
            allCitizenInfo <-
              ZIO.foreach(people)(x =>
                CareerHistoryService
                  .citizenInfo(x)
                  .map((x, _))
              )
            _ <-
              ZIO.foreach(allCitizenInfo)(
                citizenInfo =>
                  printLine(
                    "Citizen info from webserver: " +
                      citizenInfo
                  )
              )
            personEventConsumer <-
              UseKafka
                .createConsumer("person_event")
            messagesConsumed <- Ref.make(0)
            consumingPoller <-
              personEventConsumer
                .pollStream
                .foldWhileZIO(0)(
                  _ < people.length * 9
                )((x, recordsConsumed) =>
                  messagesConsumed.update(
                    _ + recordsConsumed.length
                  ) *>
                    ZIO.debug(
                      "Consumed record: " +
                        recordsConsumed
                          .map { record =>
                            record
                              .nn
                              .value
                              .toString
                          }
                          .mkString(":")
                    ) *>
                    ZIO.succeed(
                      x + recordsConsumed.length
                    )
                )
                .fork
            personEventProducer <-
              UseKafka.createProducer()
            messagesProduced <- Ref.make(0)
            _ <-
              ZIO
                .foreachParN(12)(allCitizenInfo)(
                  (citizen, citizenInfo) =>
                    personEventProducer
                      .submitForever(
                        9,
                        citizen.firstName,
                        citizenInfo,
                        "person_event",
                        messagesProduced
                      )
                )
            _ <- consumingPoller.join
            finalMessagesProduced <-
              messagesProduced.get
            finalMessagesConsumed <-
              messagesConsumed.get
            _ <-
              printLine(
                "Number of messages produced: " +
                  finalMessagesProduced
              )
            _ <-
              printLine(
                "Number of messages consumed: " +
                  finalMessagesConsumed
              )
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
                            "Joe",
                            "Joe is a dynamic baseball player!"
                          ),
                          RequestResponsePair(
                            "Shtep",
                            "Shtep has sold fizzy drinks for many years."
                          ),
                          RequestResponsePair(
                            "Zeb",
                            "Zeb worked at a machine shop."
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

        logic.provideSomeLayer[ZTestEnv & ZEnv](
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
