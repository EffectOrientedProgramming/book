package testcontainers

import org.testcontainers.containers.{
  GenericContainer,
  MockServerContainer,
  Network
}
import org.testcontainers.utility.DockerImageName
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import testcontainers.ServiceDataSets.{
  BackgroundData,
  CareerData,
  ExpectedData,
  LocationData
}
import testcontainers.proxy.{
  inconsistentFailuresZ,
  jitter,
  allProxies
}
import zio.ZLayer

// TODO Clarify the point of this example, or ditch it
trait CareerHistoryServiceT:
  def citizenInfo(
      person: String
  ): ZIO[Any, Throwable | String, String]

object CareerHistoryHardcoded:
  val live: ZLayer[
    CareerData,
    Nothing,
    CareerHistoryServiceT
  ] =
    for careerData <-
        ZLayer.service[CareerData]
    yield ZEnvironment(
      CareerHistoryHardcoded(
        careerData.get,
        inconsistentFailuresZ *> jitter
      )
    )

class CareerHistoryHardcoded private (
    pairs: CareerData,
    proxyZ: ZIO[Any, Throwable | String, Unit] =
      ZIO.unit
) extends CareerHistoryServiceT:

  def citizenInfo(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    for
      _ <-
        proxyZ
      res <-
        ZIO
          .fromOption(
            pairs
              .expectedData
              .find(_.userRequest == s"/$person")
              .map(_.response)
          )
          .mapError(_ =>
            new NoSuchElementException(
              s"No response for Person: $person"
            )
          )
    yield res
end CareerHistoryHardcoded

case class CareerHistoryServiceContainer(
    mockServerContainerZ: MockServerContainerZBasic
) extends CareerHistoryServiceT:

  def citizenInfo(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ.get(s"/$person")

object CareerHistoryService:
  def citizenInfo(person: String): ZIO[
    CareerHistoryServiceT,
    Throwable | String,
    String
  ] =
    for
      careerHistoryService <-
        ZIO.service[CareerHistoryServiceT]
      info <-
        careerHistoryService.citizenInfo(person)
    yield info

  def constructContainered[T](
      pairs: CareerData, // TODO Make this part of the environment
      proxyZ: ZIO[
        Any,
        Throwable | String,
        Unit
      ] =
        ZIO.unit
  ): ZLayer[
    Network & Clock,
    Throwable,
    CareerHistoryServiceT
  ] =
    MockServerContainerZBasic
      .construct(
        "Career History",
        pairs.expectedData,
        proxyZ
      )
      .flatMap(x =>
        ZLayer.succeed(
          CareerHistoryServiceContainer(x.get)
        )
      )

  val live: ZLayer[
    CareerData & Network,
    Throwable,
    CareerHistoryServiceT
  ] =
    for
      data <-
        ZLayer.service[CareerData]
      webserver <-
        MockServerContainerZBasic.construct(
          "Career History",
          data.get.expectedData,
          allProxies
        )
    yield ZEnvironment(
      CareerHistoryServiceContainer(
        webserver.get
      )
    )

end CareerHistoryService

// TODO Convert to trait that doesn't
// unconditionally depend on a container
class LocationService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def locationOf(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ.get(s"/$person")

object LocationService:
  def locationOf(person: String): ZIO[
    LocationService,
    Throwable | String,
    String
  ] =
    for
      locationService <-
        ZIO.service[LocationService]
      info <-
        locationService.locationOf(person)
    yield info

  val live: ZLayer[
    LocationData & Network,
    Throwable,
    LocationService
  ] =
    for
      data <-
        ZLayer.service[LocationData]
      webserver <-
        MockServerContainerZBasic.construct(
          "Location Service",
          data.get.expectedData
        )
    yield ZEnvironment(
      LocationService(webserver.get)
    )

end LocationService

class BackgroundCheckService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def criminalHistoryOf(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ.get(s"/$person")

object BackgroundCheckService:
  def criminalHistoryOf(person: String): ZIO[
    BackgroundCheckService,
    Throwable | String,
    String
  ] =
    for
      locationService <-
        ZIO.service[BackgroundCheckService]
      info <-
        locationService.criminalHistoryOf(person)
    yield s"Criminal:$info"

  val live: ZLayer[
    BackgroundData & Network,
    Throwable,
    BackgroundCheckService
  ] =
    for
      data <-
        ZLayer.service[BackgroundData]
      webserver <-
        MockServerContainerZBasic.construct(
          "BackgroundCheck Service",
          data.get.expectedData
        )
    yield ZEnvironment(
      BackgroundCheckService(webserver.get)
    )
end BackgroundCheckService
