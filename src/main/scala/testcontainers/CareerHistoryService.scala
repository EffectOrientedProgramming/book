package testcontainers

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  MockServerContainer,
  Network,
  ToxiproxyContainer
}
import org.testcontainers.utility.DockerImageName
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response

case class CareerHistoryService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def citizenInfo(
      person: Person
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ
      .get(s"/${person.firstName}")

object CareerHistoryService:
  def citizenInfo(person: Person): ZIO[Has[
    CareerHistoryService
  ], Throwable | String, String] =
    for
      careerHistoryService <-
        ZIO.service[CareerHistoryService]
      info <-
        careerHistoryService.citizenInfo(person)
    yield info

  def construct[T](
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[
    Network
  ] & Has[ToxiproxyContainer] & Has[Clock], Throwable, Has[
    CareerHistoryService
  ]] =
    MockServerContainerZBasic
      .constructProxied("Career History", pairs)
      .flatMap(x =>
        ZLayer
          .succeed(CareerHistoryService(x.get))
      )
end CareerHistoryService

class LocationService(
    mockServerContainerZ: MockServerContainerZ
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

  def construct[T](
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[Network], Throwable, Has[
    LocationService
  ]] =
    MockServerContainerZ
      .construct("Location Service", pairs)
      .flatMap(x =>
        ZLayer.succeed(LocationService(x.get))
      )
end LocationService

class BackgroundCheckService(
    mockServerContainerZ: MockServerContainerZ
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
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[Network], Throwable, Has[
    BackgroundCheckService
  ]] =
    MockServerContainerZ
      .construct(
        "BackgroundCheck Service",
        pairs
      )
      .flatMap(x =>
        ZLayer
          .succeed(BackgroundCheckService(x.get))
      )
end BackgroundCheckService
