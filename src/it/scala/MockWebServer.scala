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

object MockServerContainerZ:
  val mockSetup: (
      MockServerContainer => ZIO[
        Any,
        Throwable,
        Unit
      ]
  ) =
    mockServer =>
      ZIO.attempt {
        new MockServerClient(
          mockServer.getHost(),
          mockServer.getServerPort().nn
        ).when(
            request()
              .nn
              .withPath("/person")
              .nn
              .withQueryStringParameter(
                "name",
                "Joe"
              )
              .nn
          )
          .nn
          .respond(
            response()
              .nn
              .withBody(
                "Joe is a baseball player!"
              )
              .nn
          );
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

  def construct(): ZLayer[Has[
    Network
  ], Throwable, Has[MockServerContainer]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container = apply(network, "latest")
      res <-
        GenericInteractions
          .manageWithInitialization(
            container,
            "mockserver",
            mockSetup
          )
          .toLayer
    yield res
end MockServerContainerZ
