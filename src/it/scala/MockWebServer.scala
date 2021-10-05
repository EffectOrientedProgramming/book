package mdoc

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request;
import org.mockserver.model.HttpResponse.response;

case class RequestResponsePair(
    userRequest: String,
    response: String
)
object MockServerContainerZ:
  def apply(
      network: Network,
      version: String = "latest"
  ): MockServerContainer =
      new MockServerContainer(
        DockerImageName
          .parse(
            s"mockserver/mockserver:$version"
          )
          .nn
      ).nn
end MockServerContainerZ

class CareerHistoryService(mockServerContainer: MockServerContainer):
    def citizenInfo(person: Person): ZIO[Any, Throwable | String, String] =
      for
        responseBody <-
          ZIO.attempt {
            import sttp.client3._
            val backend =
              HttpURLConnectionBackend()
            val response =
              basicRequest
                .body("Hello, world!")
                .get(
                  uri"http://${mockServerContainer.getHost()}:${mockServerContainer.getServerPort().nn}/person/${person.firstName}"
                )
                .send(backend)

            response.body
          }
        responseBodyZ <-
          ZIO.fromEither(responseBody)
      yield responseBodyZ

object CareerHistoryService:
  def citizenInfo(person: Person): ZIO[Has[CareerHistoryService], Throwable | String, String] =
    for {
      careerHistoryService <- ZIO.service[CareerHistoryService]
      info <- careerHistoryService.citizenInfo(person)
    } yield  info

  val mockSetup: (
      MockServerContainer,
      List[RequestResponsePair]
  ) => ZIO[Any, Throwable, Unit] =
    (mockServer, requestResponsePairs) =>
      ZIO.attempt {
        requestResponsePairs.foreach {
          case RequestResponsePair(
                userName,
                userResponse
              ) =>
            new MockServerClient(
              mockServer.getHost(),
              mockServer.getServerPort().nn
            ).when(
                request()
                  .nn
                  .withPath(s"/person/$userName")
                  .nn
              )
              .nn
              .respond(
                response()
                  .nn
                  .withBody(userResponse)
                  .nn
              );
        }
      }

  def construct[T](
      pairs: List[RequestResponsePair],
  ): ZLayer[Has[Network], Throwable, Has[
    CareerHistoryService
  ]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container = MockServerContainerZ.apply(network, "latest")
      res <-
        GenericInteractions
          .manageWithInitialization(
            container,
            "mockserver",
            mockSetup(_, pairs)
          )
          .map(new CareerHistoryService(_))
                        
          .toLayer
    yield res