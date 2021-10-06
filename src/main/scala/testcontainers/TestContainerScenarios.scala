package testcontainers

import zio.*
import zio.Console.*

import java.io.IOException
import QuillLocal.AppPostgresContext

import org.testcontainers.containers.{
  GenericContainer,
  Network,
  KafkaContainer
}

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

object ScenarioRunner extends zio.ZIOAppDefault:

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

  val layer: ZLayer[Any, Throwable, Has[
    Network
  ] & Has[NetworkAwareness] & (Has[PostgresContainer] & Has[KafkaContainer]) & Has[AppPostgresContext] & Has[CareerHistoryService]] =
    ((ManagedTestInstances.networkLayer ++
      NetworkAwareness.live) >+>
      (PostgresContainer.construct("init.sql") ++
        KafkaContainerZ.construct())) >+>
      (QuillLocal
        .quillPostgresContext) ++ careerServer

  def run =
    TestContainerScenarios
      .logic
      .provideSomeLayer[ZEnv](layer)
end ScenarioRunner

object TestContainerScenarios:
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
                      record.nn.value.toString
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
        ZIO.foreachParN(12)(allCitizenInfo)(
          (citizen, citizenInfo) =>
            personEventProducer.submitForever(
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
    yield people
end TestContainerScenarios
