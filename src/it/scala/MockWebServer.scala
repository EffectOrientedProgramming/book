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

  def constructUrl(
    mockServerContainer: MockServerContainer,
    path1: String,
    path2: String,
  ) = 
    import sttp.client3._
    uri"http://${mockServerContainer.getHost()}:${mockServerContainer.getServerPort().nn}/$path1/$path2"

  val mockSetup: (
      MockServerContainer,
      List[RequestResponsePair]
  ) => ZIO[Any, Throwable, Unit] =
    (mockServer, requestResponsePairs) =>
      ZIO.attempt {
        requestResponsePairs.foreach {
          case RequestResponsePair(
                userRequest,
                userResponse
              ) =>
            new MockServerClient(
              mockServer.getHost(),
              mockServer.getServerPort().nn
            ).when(
                request()
                  .nn
                  .withPath(userRequest)
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

end MockServerContainerZ

class MockServerContainerZ(mockServerContainer: MockServerContainer):

  // TODO Include Network dependency of some kind
  def get(path1: String, path2: String): ZIO[Any, Throwable | String, String] =
      for
        responseBody <-
          ZIO.attempt {
            import sttp.client3._
              
            basicRequest
              .get(
                MockServerContainerZ.constructUrl(mockServerContainer, path1, path2)
              )
              .send(HttpURLConnectionBackend())
              .body
          }
        responseBodyZ <-
          ZIO.fromEither(responseBody)
      yield responseBodyZ

end MockServerContainerZ


class CareerHistoryService(mockServerContainerZ: MockServerContainerZ):

    def citizenInfo(person: Person): ZIO[Any, Throwable | String, String] =
      mockServerContainerZ.get("person",person.firstName)

object CareerHistoryService:
  def citizenInfo(person: Person): ZIO[Has[CareerHistoryService], Throwable | String, String] =
    for {
      careerHistoryService <- ZIO.service[CareerHistoryService]
      info <- careerHistoryService.citizenInfo(person)
    } yield  info

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
            MockServerContainerZ.mockSetup(_, pairs)
          )
          .map(new MockServerContainerZ(_))
          .map(new CareerHistoryService(_))
                        
          .toLayer
    yield res