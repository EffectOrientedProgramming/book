package testcontainers

import zio.*

import scala.jdk.CollectionConverters.*
import zio.Console.*
import org.testcontainers.containers.Network

object ContainerScenarios:
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
                yield record.value.nn +
                  s",Criminal:$criminalHistory",
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
  ], Throwable, Has[CareerHistoryService]] =
    CareerHistoryService.construct(
      List(
        RequestResponsePair(
          "/person/Joe",
          "Job:Athlete"
        ),
        RequestResponsePair(
          "/person/Shtep",
          "Job:Salesman"
        ),
        RequestResponsePair(
          "/person/Zeb",
          "Job:Mechanic"
        )
      )
    )

  val locationServer: ZLayer[Has[
    Network
  ], Throwable, Has[LocationService]] =
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
      )
    )

  val backgroundCheckServer: ZLayer[Has[
    Network
  ], Throwable, Has[BackgroundCheckService]] =
    BackgroundCheckService.construct(
      List(
        RequestResponsePair(
          "/background/Joe",
          "GoodCitizen"
        ),
        RequestResponsePair(
          "/background/Shtep",
          "Arson,DomesticViolence"
        ),
        RequestResponsePair(
          "/background/Zeb",
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
  ] & Has[NetworkAwareness] & (Has[PostgresContainerJ] & Has[KafkaContainer]) & Has[AppPostgresContext] & Has[CareerHistoryService] & Has[LocationService] & Has[BackgroundCheckService]] =
    ((networkLayer ++ NetworkAwareness.live) >+>
      (PostgresContainer.construct("init.sql") ++
        KafkaContainerZ
          .construct(topicNames))) >+>
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
