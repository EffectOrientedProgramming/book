package testcontainers

import zio.*
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
import zio.ZServiceBuilder

object NetworkAwareness:
  val localHostName: ZIO[Has[
    NetworkAwareness
  ], Throwable, String] =
    ZIO.serviceWith(_.localHostName)

  val live
      : Layer[Nothing, Has[NetworkAwareness]] =
    ZServiceBuilder.succeed(NetworkAwarenessLive)

trait NetworkAwareness:
  val localHostName: Task[String]

object NetworkAwarenessLive
    extends NetworkAwareness:
  import java.net.InetAddress
  val localHostName =
    ZIO.attempt {
      InetAddress
        .getLocalHost()
        .nn
        .getHostName()
        .nn
    }
