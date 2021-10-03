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
  val localHostName: ZIO[Has[NetworkAwareness], Throwable, String] =
    ZIO.serviceWith(_.localHostName)

  val live: Layer[Nothing, Has[NetworkAwareness]] =
    ZLayer.succeed(NetworkAwarenessLive)


object NetworkAwarenessLive extends NetworkAwareness:
  import java.net.InetAddress
  val localHostName = ZIO.attempt {
    InetAddress.getLocalHost().nn.getHostName().nn
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
            person = people.head
            _ <- printLine("Person retrieved from Postgres: " + person)
            _ <-
              MockServerClient
                .citizenInfo(person)
            personEventConsumer <- UseKafka.createConsumer( "person_event")
            consumingPoller <- personEventConsumer.pollForever().fork
            personEventProducer <- UseKafka.createProducer()
            _ <- personEventProducer.submitForever("keyX", "valueX", "person_event")
            _ <- consumingPoller.join
          yield assert(people)(
            equalTo(
              List(Person("Joe", "Dimagio", 143))
            )
          )
        import org.testcontainers.containers.MockServerContainer

        val layer =
          ((ManagedTestInstances.networkLayer ++ NetworkAwareness.live) >+>
            (PostgresContainer
              .construct("init.sql") ++
              KafkaContainerZ.construct())) >+>
            (QuillLocal.quillPostgresContext) ++
            (MockServerContainerZ.construct(List(RequestResponsePair("Joe", "Joe is a dynamic baseball player!"))))

        logic.provideSomeLayer[ZTestEnv & ZEnv](
          layer
        )
      }
    )
end TestContainersSpec

object MdocSession:
  object App:
    case class GpsException()
        extends RuntimeException
