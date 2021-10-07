package testcontainers

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
import org.testcontainers.containers.KafkaContainer

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
end ContainerScenarios
