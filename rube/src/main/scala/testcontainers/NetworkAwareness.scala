package testcontainers

import zio.Console.*
import zio.test.*
import zio.test.Assertion.*
import java.io.IOException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import io.getquill._
import org.testcontainers.containers.KafkaContainer
import java.net.InetAddress
import zio.ZLayer

object NetworkAwareness:
  val localHostName: ZIO[
    NetworkAwareness,
    Throwable,
    String
  ] =
    ZIO
      .service[NetworkAwareness]
      .flatMap(_.localHostName)

  val live: Layer[Nothing, NetworkAwareness] =
    ZLayer.succeed(NetworkAwarenessLive)

trait NetworkAwareness:
  val localHostName: Task[String]

object NetworkAwarenessLive
    extends NetworkAwareness:
  import java.net.InetAddress
  val localHostName =
    ZIO.attempt(
      InetAddress.getLocalHost().getHostName()
    )
