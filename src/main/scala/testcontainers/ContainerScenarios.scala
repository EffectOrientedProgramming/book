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
      personEventConsumer: KafkaConsumerZ <-
        UseKafka.createConsumer("person_event")

      housingHistoryConsumer <-
        UseKafka
          .createConsumer("housing_history")

      personEventProducer <-
        UseKafka.createProducer("person_event")

      personEventToLocationStream <-
        UseKafka
          .createForwardedStreamZ(
            topicName = "person_event",
            op = record =>
              for
              // TODO Get rid of person lookup
              // and pass plain String name to
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
            outputTopicName = "housing_history"
          )
          // I had to increase the timeout since
          // I'm creating the output producer
          // internally now.
          // TODO Consider the ramifications of
          // this...
          .timeout(10.seconds).fork

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
          .timeout(5.seconds)
          .fork

      _ <- ZIO.sleep(1.second)

      producer <-
        ZIO
          .foreachParN(12)(allCitizenInfo)(
            (citizen, citizenInfo) =>
              personEventProducer.submitForever(
                citizen.firstName,
                s"${citizen.firstName},${citizenInfo}"
              )
          )
          .timeout(4.seconds)
          .fork

      _ <- producer.join
//      _ <- consumingPoller.join
      _ <- personEventToLocationStream.join
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
          personEventConsumer
            .messagesConsumed
            .get,
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

  import testcontainers.QuillLocal.AppPostgresContext

  import org.testcontainers.containers.KafkaContainer
  val layer: ZLayer[Any, Throwable, Has[
    Network
  ] & Has[NetworkAwareness] & (Has[PostgresContainerJ] & Has[KafkaContainer]) & Has[AppPostgresContext] & Has[CareerHistoryService] & Has[LocationService]] =
    ((networkLayer ++ NetworkAwareness.live) >+>
      (PostgresContainer.construct("init.sql") ++
        KafkaContainerZ.construct())) >+>
      (QuillLocal.quillPostgresContext) ++
      careerServer ++ locationServer

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
