package testcontainers

import zio.*

import scala.jdk.CollectionConverters.*
import zio.Console.*
import org.testcontainers.containers.Network
import testcontainers.proxy.{
  inconsistentFailuresZ,
  jitter
}
import testcontainers.QuillLocal.AppPostgresContext
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MockServerContainer
import testcontainers.ServiceDataSets.{
  BackgroundData,
  CareerData
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
        .tapError(reportTopLevelError)
    _ <- printLine("Result: " + result)
  yield ()

object ProxiedRequestScenario
    extends zio.ZIOAppDefault:
  def run =
    makeAProxiedRequest
      .provideSomeLayer[ZEnv](liveLayer)

  val careerServer: ZLayer[Has[
    Network
  ] & Has[Clock] & Has[CareerData], Throwable, Has[
    CareerHistoryServiceT
  ]] =
    CareerHistoryService.constructContainered(
      ServiceDataSets.careerData,
      inconsistentFailuresZ
    )

  val liveLayer: ZLayer[
    Any,
    Throwable,
    Deps.AppDependencies
  ] =
    ZLayer.wire[Deps.AppDependencies](
      ServiceDataSets.careerDataZ,
      Clock.live,
      Layers.networkLayer,
      NetworkAwareness.live,
      ToxyProxyContainerZ.construct(),
      careerServer
    )

end ProxiedRequestScenario

object ProxiedRequestScenarioUnit
    extends zio.ZIOAppDefault:

  def run =
    makeAProxiedRequest
      .provideSomeLayer[ZEnv](liveLayer)

  val liveLayer =
    ServiceDataSets.careerDataZ >>>
      CareerHistoryHardcoded.live

def reportTopLevelError(
    failure: Throwable | String
) =
  val errorMsg =
    failure match
      case t: Throwable =>
        t.getCause.nn.getMessage
      case s: String =>
        s
  printLine("Failure: " + failure) *>
    printLine(errorMsg)

object ContainerScenarios:
  val logic =
    for
      people <- QuillLocal.quillQuery
      allCitizenInfo <-
        ZIO.foreach(people)(x =>
          CareerHistoryService
            .citizenInfo(x)
            .tapError(reportTopLevelError)
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
                citizen.firstName,
                s"${citizen.firstName},${citizenInfo}"
              )
          )
          .timeout(4.seconds)
          .fork

      _ <- producer.join
      _ <- criminalHistoryStream.join
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
    yield people

  val careerServer: ZLayer[Has[
    Network
  ] & Has[Clock], Throwable, Has[
    CareerHistoryServiceT
  ]] =
    CareerHistoryService.constructContainered(
      ServiceDataSets.careerData
    )

  val backgroundCheckServer: ZLayer[Has[
    Network
  ] & Has[BackgroundData], Throwable, Has[
    BackgroundCheckService
  ]] = BackgroundCheckService.live

  val topicNames =
    List(
      "person_event",
      "housing_history",
      "criminal_history"
    )

  val layer =
    ZLayer.wire[Deps.RubeDependencies](
      ServiceDataSets.careerDataZ,
      ServiceDataSets.locations,
      ServiceDataSets.backgroundData,
      Clock.live,
      Layers.networkLayer,
      NetworkAwareness.live,
      PostgresContainer.construct("init.sql"),
      KafkaContainerZ.construct(topicNames),
      ToxyProxyContainerZ.construct(),
      QuillLocal.quillPostgresContext,
      careerServer,
      LocationService.live,
      backgroundCheckServer
    )

end ContainerScenarios

object RunScenarios extends zio.ZIOAppDefault:
  def run =
    ContainerScenarios
      .logic
      .provideSomeLayer[ZEnv](
        ContainerScenarios.layer
      )
