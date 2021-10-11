package testcontainers

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response

class CareerHistoryService(
    mockServerContainerZ: MockServerContainerZ
):

  def citizenInfo(
      person: Person
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ
      .get(s"/person/${person.firstName}")

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
  ): ZLayer[Has[Network], Throwable, Has[
    CareerHistoryService
  ]] =
    MockServerContainerZ
      .construct(pairs)
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
      .get(s"/location/${person.firstName}")

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
      .construct(pairs)
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
      .get(s"/background/${person.firstName}")

object BackgroundCheckService:
  def criminalHistoryOf(person: Person): ZIO[Has[
    BackgroundCheckService
  ], Throwable | String, String] =
    for
      locationService <-
        ZIO.service[BackgroundCheckService]
      info <-
        locationService.criminalHistoryOf(person)
    yield info

  def construct[T](
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[Network], Throwable, Has[
    BackgroundCheckService
  ]] =
    MockServerContainerZ
      .construct(pairs)
      .flatMap(x =>
        ZLayer
          .succeed(BackgroundCheckService(x.get))
      )
end BackgroundCheckService
