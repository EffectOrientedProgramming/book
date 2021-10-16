package testcontainers

import zio.*
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
  jitter
}

trait CareerHistoryServiceT:
  def citizenInfo(
      person: Person
  ): ZIO[Any, Throwable | String, String]

object CareerHistoryHardcoded:
  val live: ZLayer[Has[CareerData], Nothing, Has[
    CareerHistoryServiceT
  ]] =
    for
      careerData <- ZLayer.service[CareerData]
    yield Has(
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
      person: Person
  ): ZIO[Any, Throwable | String, String] =
    for
      _ <- proxyZ
      res <-
        ZIO
          .fromOption(
            pairs
              .expectedData
              .find(
                _.userRequest ==
                  s"/${person.firstName}"
              )
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
      person: Person
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ
      .get(s"/${person.firstName}")

object CareerHistoryService:
  def citizenInfo(person: Person): ZIO[Has[
    CareerHistoryServiceT
  ], Throwable | String, String] =
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
      ] = ZIO.unit
  ): ZLayer[Has[
    Network
  ] & Has[Clock], Throwable, Has[
    CareerHistoryServiceT
  ]] =
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

end CareerHistoryService

// TODO Convert to trait that doesn't
// unconditionally depend on a container
class LocationService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def locationOf(
      person: Person
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ
      .get(s"/${person.firstName}")

object LocationService:
  def locationOf(person: Person): ZIO[Has[
    LocationService
  ], Throwable | String, String] =
    for
      locationService <-
        ZIO.service[LocationService]
      info <- locationService.locationOf(person)
    yield info

  val live: ZLayer[Has[
    LocationData
  ] & Has[Network], Throwable, Has[
    LocationService
  ]] =
    for
      data <- ZLayer.service[LocationData]
      webserver: Has[
        MockServerContainerZBasic
      ] <-
        MockServerContainerZBasic.construct(
          "Location Service",
          data.get.expectedData
        )
    yield Has(LocationService(webserver.get))

end LocationService

class BackgroundCheckService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def criminalHistoryOf(
      person: Person
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ
      .get(s"/${person.firstName}")

object BackgroundCheckService:
  def criminalHistoryOf(person: Person): ZIO[Has[
    BackgroundCheckService
  ], Throwable | String, String] =
    for
      locationService <-
        ZIO.service[BackgroundCheckService]
      info <-
        locationService.criminalHistoryOf(person)
    yield s"Criminal:$info"

  def construct[T](
      pairs: BackgroundData
  ): ZLayer[Has[Network], Throwable, Has[
    BackgroundCheckService
  ]] =
    MockServerContainerZBasic
      .construct(
        "BackgroundCheck Service",
        pairs.expectedData
      )
      .flatMap(x =>
        ZLayer
          .succeed(BackgroundCheckService(x.get))
      )
end BackgroundCheckService
