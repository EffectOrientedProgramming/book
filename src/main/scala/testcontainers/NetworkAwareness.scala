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
import org.testcontainers.containers.KafkaContainer

trait NetworkAwareness:
  val localHostName: Task[String]

object NetworkAwareness:
  val localHostName: ZIO[Has[
    NetworkAwareness
  ], Throwable, String] =
    ZIO.serviceWith(_.localHostName)

  val live
      : Layer[Nothing, Has[NetworkAwareness]] =
    ZLayer.succeed(NetworkAwarenessLive)

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
