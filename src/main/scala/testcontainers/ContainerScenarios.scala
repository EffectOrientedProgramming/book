package testcontainers

import zio.*
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
      personEventConsumer <-
        UseKafka.createConsumer("person_event")

      housingHistoryConsumer <-
        UseKafka
          .createConsumer("housing_history")

      messagesProduced <- Ref.make(0)
      personEventProducer <-
        UseKafka.createProducer(messagesProduced)

      messagesConsumed <- Ref.make(0)

      consumingPoller <-
      ZIO.sleep(1.second) *>
        personEventConsumer
          .pollStream(messagesConsumed)
          .foreach(recordsConsumed =>
            ZIO
              .foreach(recordsConsumed)(record =>
                // ZIO.debug(
                // s"Submitting: ${record.key}
                // Value: ${record.value} "
                // ) *>
                personEventProducer.submit(
                  record.key.nn,
                  record.value.nn,
                  "housing_history"
                )
              )
          )
          .timeout(5.seconds)
          .fork

      consumingPoller2 <-
      ZIO.sleep(1.second) *>
        housingHistoryConsumer
          .pollStream(messagesConsumed)
          .foreach(recordsConsumed =>
            ZIO
              .foreach(recordsConsumed)(record =>
                val person =
                  people
                    .find(
                      _.firstName ==
                        record.key.nn
                    )
                    .get // TODO Unsafe!
                for
                  location <-
                    LocationService
                      .locationOf(person)
                  _ <-
                    printLine(
                      s"Location of $person: $location"
                    )
                yield ()
              )
          )
          .timeout(5.seconds)
          .fork

      _ <-
        ZIO
          .foreachParN(12)(allCitizenInfo)(
            (citizen, citizenInfo) =>
              personEventProducer.submitForever(
                citizen.firstName,
                s"${citizen.firstName},${citizenInfo}",
                "person_event"
              )
          )
          .timeout(4.seconds)
      _ <- consumingPoller.join
      _ <- consumingPoller2.join
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

object RunScenarios extends zio.ZIOAppDefault:
  def run =
    ContainerScenarios
      .logic
      .provideSomeLayer[ZEnv](
        ContainerScenarios.layer
      )
