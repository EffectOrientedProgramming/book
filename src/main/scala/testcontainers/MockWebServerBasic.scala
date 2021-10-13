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
import org.mockserver.model.HttpResponse.response;

object MockServerContainerZBasic:

  def constructProxied[T](
      serviceName: String,
      pairs: List[RequestResponsePair]
  ): ZLayer[Has[ToxiproxyContainer] with Has[
    Network
  ] & Has[Clock], Throwable, Has[
    MockServerContainerZBasic
  ]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      _ <- ZIO.debug("XXX").toLayer
      toxi <-
        ZLayer
          .service[ToxiproxyContainer]
          .map(_.get)
      _ <- ZIO.sleep(2.seconds).toLayer
//      _ <- ZIO.debug("YYY").toLayer
      container =
        MockServerContainerZBasic
          .apply(network, "latest")
      // TODO move toxi access into
      // ToxyProxyContainerZ
      _ <- ZIO.debug("ZZZ").toLayer
      res <-
        GenericInteractionsZ
          .manageWithInitialization(
            container,
            s"$serviceName mockserver",
            c =>
              MockServerContainerZBasic
                .mockSetup(c, pairs)
          )
          .map { mockContainer =>
            val proxy: Int =
              ToxyProxyContainerZ
                .use(toxi, container)
            println("Proxy port: " + proxy)

            new MockServerContainerZBasic(
              container
                .getHost
                .nn, // TODO Assumes proxy and server are on same host
              proxy
            )
          }
          .toLayer
    yield res
    end for
  end constructProxied

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
      host: String,
      serverPort: Int,
      path: String
  ) =
    import sttp.client3._
    val uriString =
      s"http://$host:$serverPort$path"
    uri"$uriString"

  private val mockSetup: (
      MockServerContainer,
      List[RequestResponsePair]
  ) => ZIO[Any, Throwable, Unit] =
    (mockServer, requestResponsePairs) =>
      ZIO.debug("Starting to mock server") *>
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
        } *> ZIO.debug("Finished mock server")

end MockServerContainerZBasic

class MockServerContainerZBasic(
    host: String,
    serverPort: Int
):
  println("Host for mock server: " + host)

  // TODO Include Network dependency of some kind
  def get(
      path: String
  ): ZIO[Any, Throwable | String, String] =
    for
      responseBody <-
        ZIO.attempt {
          import sttp.client3.{
            HttpURLConnectionBackend,
            basicRequest
          }

          basicRequest
            .get(
              MockServerContainerZBasic
                .constructUrl(
                  host,
                  serverPort,
                  path
                )
            )
            .send(HttpURLConnectionBackend())
            .body
        }
      responseBodyZ <-
        ZIO.fromEither(responseBody)
    yield responseBodyZ

end MockServerContainerZBasic
