package mdoc

import zio.*
import zio.Console.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*
import java.io.IOException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import io.getquill._
import mdoc.QuillLocal.AppPostgresContext
import org.testcontainers.containers.KafkaContainer
import java.net.InetAddress

object NetworkAwareness:
  val localHostName: ZIO[Has[
    NetworkAwareness
  ], Throwable, String] =
    ZIO.serviceWith(_.localHostName)

  val live
      : Layer[Nothing, Has[NetworkAwareness]] =
    ZLayer.succeed(NetworkAwarenessLive)

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