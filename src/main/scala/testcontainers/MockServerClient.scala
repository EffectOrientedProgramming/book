package testcontainers

import zio.*
import zio.Console.*
import java.io.IOException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import io.getquill._
import org.testcontainers.containers.MockServerContainer

object MockServerClient:
  def citizenInfo(person: Person): ZIO[Has[
    MockServerContainer
  ], Throwable | String, String] =
    for
      mockServerContainer <-
        ZIO.service[MockServerContainer]
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

end MockServerClient
