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

  def apply(
      network: Network,
      version: String = "latest"
  ): MockServerContainer =
    val container =
      new MockServerContainer(
        DockerImageName
          .parse(
            s"mockserver/mockserver:$version"
          )
          .nn
      ).nn
    container

  def construct(
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[Network], Throwable, Has[
    MockServerContainer
  ]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container = apply(network, "latest")
      res <-
        GenericInteractions
          .manageWithInitialization(
            container,
            "mockserver",
            mockSetup(_, pairs)
          )
          .toLayer
    yield res
end MockServerContainerZ
