package testcontainers

import zio.*

import scala.jdk.CollectionConverters.*
import zio.Console.*
import org.testcontainers.containers.{
  Network,
  ToxiproxyContainer
}

case class SuspectProfile(
    name: String,
    criminalHistory: Option[String]
)

val makeAProxiedRequest =
  for
    result <-
      CareerHistoryService
        .citizenInfo(Person("Zeb", "Zestie", 27))
        .tapError { failure =>
          val errorMsg =
            failure match
              case t: Throwable =>
                t.getCause.nn.getMessage
              case s: String =>
                s
          printLine("Failure: " + failure) *>
            printLine(errorMsg) *>
            ZIO.sleep(30.seconds)
        }
  yield ()

object ProxiedRequestScenario
    extends zio.ZIOAppDefault:
  def run =
    makeAProxiedRequest.provideSomeLayer[ZEnv](
      liveLayer(proxied = true)
    )

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

  val careerServer: ZLayer[Has[
    Network
  ] & Has[ToxiproxyContainer] & Has[Clock], Throwable, Has[
    CareerHistoryService
  ]] =
    CareerHistoryService.construct(
      List(
        RequestResponsePair(
          "/Joe",
          "Job:Athlete"
        ),
        RequestResponsePair(
          "/Shtep",
          "Job:Salesman"
        ),
        RequestResponsePair(
          "/Zeb",
          "Job:Mechanic"
        )
      )
    )

  val careerServerProxied: ZLayer[Has[
    Network
  ] & Has[ToxiproxyContainer] & Has[Clock], Throwable, Has[
    CareerHistoryService
  ]] =
    CareerHistoryService.constructProxied(
      List(
        RequestResponsePair(
          "/Joe",
          "Job:Athlete"
        ),
        RequestResponsePair(
          "/Shtep",
          "Job:Salesman"
        ),
        RequestResponsePair(
          "/Zeb",
          "Job:Mechanic"
        )
      )
    )

  import testcontainers.QuillLocal.AppPostgresContext
  import org.testcontainers.containers.KafkaContainer
  def liveLayer(
      proxied: Boolean
  ): ZLayer[Any, Throwable, Has[
    Network
  ] & Has[NetworkAwareness] & Has[CareerHistoryService] & Has[ToxiproxyContainer]] =
    (Clock.live ++ networkLayer ++
      NetworkAwareness.live) >+>
      ToxyProxyContainerZ.construct() >+>
      (if (proxied)
         careerServerProxied
       else
         careerServer)
end ProxiedRequestScenario

object ContainerScenarios:
  val logic =
    for
      // careerService <-
      // ZIO.service[CareerHistoryService]
      // _ <- careerService.mockServerContainerZ
      _ <- printLine("Doing any logic at all...")
      people <- QuillLocal.quillQuery
      allCitizenInfo <-
        ZIO.foreach(people)(x =>
          CareerHistoryService
            .citizenInfo(x)
            .tapError { failure =>
              val errorMsg =
                failure match
                  case t: Throwable =>
                    t.getCause.nn.getMessage
                  case s: String =>
                    s
              printLine("Failure: " + failure) *>
                printLine(errorMsg) *>
                ZIO.sleep(30.seconds)
            }
            .map((x, _))
        )
      _ <-
        ZIO
          .foreach(allCitizenInfo)(citizenInfo =>
            printLine(
              "Citizen info from webserver: " +
                citizenInfo
            )
          )

      housingHistoryConsumer <-
        UseKafka.createConsumer(
          "housing_history",
          "housing"
        )

      criminalHistoryConsumer <-
        UseKafka.createConsumer(
          "criminal_history",
          "criminal"
        )

      personEventProducer <-
        UseKafka.createProducer("person_event")

//      _ <- ZIO.forkAll(???)
      personEventToLocationStream <-
        UseKafka
          .createForwardedStreamZ(
            topicName = "person_event",
            op =
              record =>
                for
                  // TODO Get rid of person
                  // lookup
                  // and pass plain String name
                  // to
                  // LocationService
                  person <-
                    ZIO.fromOption(
                      people.find(
                        _.firstName ==
                          record.key.nn
                      )
                    )
                  location <-
                    LocationService
                      .locationOf(person)
                yield record.value.nn +
                  s",Location:$location",
            outputTopicName = "housing_history",
            groupId = "housing"
          )
          .timeout(10.seconds)
          .fork

      criminalHistoryStream <-
        UseKafka
          .createForwardedStreamZ(
            topicName = "person_event",
            op =
              record =>
                for
                  // TODO Get rid of person
                  // lookup
                  // and pass plain String name
                  // to
                  // LocationService
                  person <-
                    ZIO.fromOption(
                      people.find(
                        _.firstName ==
                          record.key.nn
                      )
                    )
                  criminalHistory <-
                    BackgroundCheckService
                      .criminalHistoryOf(person)
                yield s"${record.value.nn},$criminalHistory",
            outputTopicName = "criminal_history",
            groupId = "criminal"
          )
          .timeout(10.seconds)
          .fork

      consumingPoller2 <-
        housingHistoryConsumer
          .pollStream()
          .foreach(recordsConsumed =>
            ZIO
              .foreach(recordsConsumed)(record =>
                val location: String =
                  RecordManipulation
                    .getField("Location", record)
                printLine(
                  s"Location of ${record.key}: $location"
                )
              )
          )
          .timeout(10.seconds)
          .fork

      criminalPoller <-
        criminalHistoryConsumer
          .pollStream()
          .foreach(recordsConsumed =>
            ZIO
              .foreach(recordsConsumed)(record =>
                ZIO.debug(
                  "Criminal History record:" +
                    record.value.nn
                ) *> {
                  val location: String =
                    RecordManipulation.getField(
                      "Criminal",
                      record
                    )
                  printLine(
                    s"History of ${record.key}: $location"
                  )
                }
              )
          )
          .timeout(10.seconds)
          .fork

      _ <- ZIO.sleep(1.second)

      producer <-
        ZIO
          .foreachParN(12)(allCitizenInfo)(
            (citizen, citizenInfo) =>
              personEventProducer.submit(
// personEventProducer.submitForever(
                citizen.firstName,
                s"${citizen.firstName},${citizenInfo}"
              )
          )
          .timeout(4.seconds)
          .fork

      _ <- producer.join
      _ <- criminalHistoryStream.join
//      _ <- consumingPoller.join
      _ <- personEventToLocationStream.join
      _ <- criminalPoller.join
      _ <- consumingPoller2.join
      finalMessagesProduced <-
        ZIO.reduceAll(
          ZIO.succeed(1),
          List(
            personEventProducer
              .messagesProduced
              .get
          )
        )(_ + _)

      finalMessagesConsumed <-
        ZIO.reduceAll(
          ZIO.succeed(0),
          List(
            housingHistoryConsumer
              .messagesConsumed
              .get
          )
        )(_ + _)
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
    yield people

  import org.testcontainers.containers.MockServerContainer

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

  val careerServer: ZLayer[Has[
    Network
  ] & Has[ToxiproxyContainer] & Has[Clock], Throwable, Has[
    CareerHistoryService
  ]] =
    CareerHistoryService.construct(
      List(
        RequestResponsePair(
          "/Joe",
          "Job:Athlete"
        ),
        RequestResponsePair(
          "/Shtep",
          "Job:Salesman"
        ),
        RequestResponsePair(
          "/Zeb",
          "Job:Mechanic"
        )
      )
    )

  val locationServer: ZLayer[Has[
    Network
  ], Throwable, Has[LocationService]] =
    LocationService.construct(
      List(
        RequestResponsePair("/Joe", "USA"),
        RequestResponsePair("/Shtep", "Jordan"),
        RequestResponsePair("/Zeb", "Taiwan")
      )
    )

  val backgroundCheckServer: ZLayer[Has[
    Network
  ], Throwable, Has[BackgroundCheckService]] =
    BackgroundCheckService.construct(
      List(
        RequestResponsePair(
          "/Joe",
          "GoodCitizen"
        ),
        RequestResponsePair(
          "/Shtep",
          "Arson,DomesticViolence"
        ),
        RequestResponsePair(
          "/Zeb",
          "SpeedingTicket"
        )
      )
    )

  import testcontainers.QuillLocal.AppPostgresContext
  val topicNames =
    List(
      "person_event",
      "housing_history",
      "criminal_history"
    )

  import org.testcontainers.containers.KafkaContainer
  val layer: ZLayer[Any, Throwable, Has[
    Network
  ] & Has[NetworkAwareness] & (Has[PostgresContainerJ] & Has[KafkaContainer]) & Has[AppPostgresContext] & Has[CareerHistoryService] & Has[LocationService] & Has[BackgroundCheckService] & Has[ToxiproxyContainer]] =
    ((Clock.live ++ networkLayer ++
      NetworkAwareness.live) >+>
      (PostgresContainer.construct("init.sql") ++
        KafkaContainerZ.construct(topicNames)) ++
      ToxyProxyContainerZ.construct()) >+>
      (QuillLocal.quillPostgresContext) ++
      careerServer ++
      locationServer ++ backgroundCheckServer

end ContainerScenarios

object RecordManipulation:
  import org.apache.kafka.clients.consumer.ConsumerRecord
  def getField(
      fieldName: String,
      record: ConsumerRecord[String, String]
  ) =
    val fields: List[String] =
      java
        .util
        .Arrays
        .asList(
          record.value.nn.split(",").nn.map(_.nn)
        )
        .nn
        .asScala
        .last
        .toList

    val field: String =
      fields
        .find(_.startsWith(fieldName))
        .getOrElse(
          throw new IllegalArgumentException(
            s"Bad fieldName : $fieldName"
          )
        )

    field.dropWhile(_ != ':').drop(1)

  end getField
end RecordManipulation

object RunScenarios extends zio.ZIOAppDefault:
  def run =
    ContainerScenarios
      .logic
      .provideSomeLayer[ZEnv](
        ContainerScenarios.layer
      )
