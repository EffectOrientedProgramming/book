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
import testcontainers.ServiceDataSets.ExpectedData

import java.net.SocketException;

case class RequestResponsePair(
    userRequest: String,
    response: String
)

object MockServerContainerZBasic:

  def construct[T](
      serviceName: String,
      pairs: ExpectedData,
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
      ExpectedData
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
