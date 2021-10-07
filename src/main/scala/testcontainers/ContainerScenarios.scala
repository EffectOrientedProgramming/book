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

      personEventProducer <-
        UseKafka.createProducer()

      messagesConsumed <- Ref.make(0)

      consumingPoller <-
        personEventConsumer
          .pollStream(messagesConsumed)
          .foldZIO(0)((x, recordsConsumed) =>
            ZIO.debug(
              "Consumed record: " +
                recordsConsumed
                  .map { record =>
                    record.nn.value.toString
                  }
                  .mkString(":")
            ) *>
              ZIO.foreach(recordsConsumed)(
                record =>
                  personEventProducer.submit(
                    record.key.nn,
                    "HousingHistory: " +
                      record.value.nn,
                    "housing_history"
                  )
              ) *> ZIO.succeed(0)
          )
          .timeout(5.seconds)
          .fork

      consumingPoller2 <-
        housingHistoryConsumer
          .pollStream(messagesConsumed)
          .foldZIO(0)((x, recordsConsumed) =>
            ZIO.debug(
              "Consumed record: " +
                recordsConsumed
                  .map { record =>
                    record.nn.value.toString
                  }
                  .mkString(":")
            ) *> ZIO.succeed(0)
          )
          .timeout(5.seconds)
          .fork

      messagesProduced <- Ref.make(0)
      _ <-
        ZIO
          .foreachParN(12)(allCitizenInfo)(
            (citizen, citizenInfo) =>
              personEventProducer.submitForever(
                citizen.firstName,
                citizenInfo,
                "person_event",
                messagesProduced
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
  ] & Has[NetworkAwareness] & (Has[PostgresContainerJ] & Has[KafkaContainer]) & Has[AppPostgresContext] & Has[CareerHistoryService]] =
    ((networkLayer ++ NetworkAwareness.live) >+>
      (PostgresContainer.construct("init.sql") ++
        KafkaContainerZ.construct())) >+>
      (QuillLocal
        .quillPostgresContext) ++ careerServer

end ContainerScenarios

object RunScenarios extends zio.ZIOAppDefault:
  def run =
    ContainerScenarios
      .logic
      .provideSomeLayer[ZEnv](
        ContainerScenarios.layer
      )
