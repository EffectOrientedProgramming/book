package mdoc

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import org.apache.kafka.clients.admin.NewTopic
import fansi.Str
import java.net.InetAddress
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Instant
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.clients.producer.RecordMetadata
import scala.concurrent.java8.FuturesConvertersImpl.P

object GenericInteractions:
  def manage[T <: GenericContainer[T]](
      c: T,
      containerType: String
  ) =
    ZManaged.acquireReleaseWith(
      ZIO.debug(s"Creating $containerType") *>
        start(c, containerType) *> ZIO.succeed(c)
    )((n: T) =>
      ZIO.attempt(n.close()).orDie *>
        ZIO.debug(s"Closing $containerType")
    )

  def manageWithInitialization[
      T <: GenericContainer[T]
  ](
      c: T,
      containerType: String,
      initialize: T => ZIO[
        Any,
        Throwable,
        Unit
      ] = (_: T) => ZIO.unit
  ): ZManaged[Any, Throwable, T] =
    ZManaged.acquireReleaseWith(
      ZIO.debug(s"Creating $containerType") *>
        start(c, containerType) *>
        initialize(c) *> ZIO.succeed(c)
    )((n: T) =>
      ZIO.attempt(n.close()).orDie *>
        ZIO.debug(s"Closing $containerType")
    )

  private def start[T <: GenericContainer[T]](
      c: T,
      containerType: String
  ) =
    ZIO.blocking(ZIO.succeed(c.start)) *>
      ZIO.debug(
        s"Finished blocking for $containerType container creation"
      )

end GenericInteractions
