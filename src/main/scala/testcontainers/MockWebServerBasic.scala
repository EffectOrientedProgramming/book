package testcontainers

import zio.*
import zio.Console.printLine
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
import sttp.client3.SttpClientException.{
  ConnectException,
  ReadException
}

import java.net.SocketException;

case class RequestResponsePair(
    userRequest: String,
    response: String
)

object MockServerContainerZBasic:

  // TODO Debug this in particular
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
            val proxyPort: Int =
              ToxyProxyContainerZ
                .createProxiedLink(
                  toxi,
                  container
                )
            println("Proxy port: " + proxyPort)

            new MockServerContainerZBasic(
              container
                .getHost
                .nn, // TODO Assumes proxy and server are on same host
              proxyPort,
              ZIO.unit
            )
          }
          .toLayer
    yield res
    end for
  end constructProxied

  def construct[T](
      serviceName: String,
      pairs: List[RequestResponsePair],
      proxyZ: ZIO[
        Any,
        Throwable | String,
        Unit
      ] = ZIO.unit
  ): ZLayer[Has[Network], Throwable, Has[
    MockServerContainerZBasic
  ]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container =
        MockServerContainerZBasic
          .apply(network, "latest")
      res <-
        GenericInteractionsZ
          .manageWithInitialization(
            container,
            s"$serviceName mockserver",
            MockServerContainerZBasic
              .mockSetup(_, pairs)
          )
          .map(mockServerContainer =>
            new MockServerContainerZBasic(
              mockServerContainer.getHost.nn,
              mockServerContainer
                .getServerPort
                .nn,
              proxyZ
            )
          )
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

  def constructUrl(
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

trait MockServer:
  def get(
      path: String
  ): ZIO[Any, Throwable | String, String]

class MockServerContainerZBasic(
    host: String,
    serverPort: Int,
    proxyZ: ZIO[Any, Throwable | String, Unit]
) extends MockServer:

  override def get(
      path: String
  ): ZIO[Any, Throwable | String, String] =
    for
      _ <- proxyZ
      response <-
        try
          ZIO
            .attempt {
              import sttp.client3.{
                HttpURLConnectionBackend,
                basicRequest
              }
              val r =
                basicRequest
                  .get(
                    MockServerContainerZBasic
                      .constructUrl(
                        host,
                        serverPort,
                        path
                      )
                  )
                  .send(
                    HttpURLConnectionBackend()
                  )
              r
            }
            .tapError { x =>
              import sttp.client3.SttpClientException
              (
                x match
                  case err: ConnectException =>
                    ZIO.debug(
                      "ConnectException: " + err
                    )
                  case err: ReadException =>
                    ZIO.debug(
                      "ReadException: " + err
                    )
                  case _ =>
                    ZIO.unit
              )
            }
        catch
          case defect =>
            ZIO.debug("Defect: " + defect) *>
              ZIO.fail(defect)
      _ <- ZIO.debug(response.code)
      responseBodyZ <-
        ZIO.fromEither(response.body)
    yield responseBodyZ

end MockServerContainerZBasic
