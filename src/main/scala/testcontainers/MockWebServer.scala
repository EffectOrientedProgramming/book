package testcontainers

import zio.*
import zio.Console.printLine
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

  def construct[T](
      serviceName: String,
      pairs: List[RequestResponsePair]
  ) =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container =
        MockServerContainerZ
          .apply(network, "latest")
      res <-
        GenericInteractionsZ
          .manageWithInitialization(
            container,
            s"$serviceName mockserver",
            MockServerContainerZ
              .mockSetup(_, pairs)
          )
          .map(new MockServerContainerZ(_))
          .toLayer
    yield res

  private def apply(
      network: Network,
      version: String = "latest"
  ): MockServerContainer =
    new MockServerContainer(
      DockerImageName
        .parse(s"mockserver/mockserver:$version")
        .nn
    ).nn

  private def constructUrl(
      mockServerContainer: MockServerContainer,
      path: String
  ) =
    import sttp.client3._
    val uriString =
      s"http://${mockServerContainer.getHost()}:${mockServerContainer.getServerPort().nn}$path"
    uri"$uriString"

  private val mockSetup: (
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

class MockServerContainerZ(
    mockServerContainer: MockServerContainer
):

  // TODO Include Network dependency of some kind
  def get(
      path: String
  ): ZIO[Any, Throwable | String, String] =
    for
      response <-
        try
          ZIO.attempt {
            import sttp.client3.{
              HttpURLConnectionBackend,
              basicRequest
            }
            basicRequest
              .get(
                MockServerContainerZ
                  .constructUrl(
                    mockServerContainer,
                    path
                  )
              )
              .send(HttpURLConnectionBackend())
          }
        catch
          case defect =>
            ZIO.debug(defect) *> ZIO.fail(defect)
      _ <- ZIO.debug(response.code)
      responseBodyZ <-
        ZIO.fromEither(response.body)
    yield responseBodyZ

end MockServerContainerZ
