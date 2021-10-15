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

trait CareerHistoryServiceT:
  def citizenInfo(
      person: Person
  ): ZIO[Any, Throwable | String, String]

class CareerHistoryHardcoded(
    pairs: List[RequestResponsePair],
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
      pairs: List[RequestResponsePair],
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
      .construct("Career History", pairs, proxyZ)
      .flatMap(x =>
        ZLayer.succeed(
          CareerHistoryServiceContainer(x.get)
        )
      )

end CareerHistoryService

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

  def construct[T](
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[Network], Throwable, Has[
    LocationService
  ]] =
    MockServerContainerZBasic
      .construct("Location Service", pairs)
      .flatMap(x =>
        ZLayer.succeed(LocationService(x.get))
      )
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
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[Network], Throwable, Has[
    BackgroundCheckService
  ]] =
    MockServerContainerZBasic
      .construct(
        "BackgroundCheck Service",
        pairs
      )
      .flatMap(x =>
        ZLayer
          .succeed(BackgroundCheckService(x.get))
      )
end BackgroundCheckService
