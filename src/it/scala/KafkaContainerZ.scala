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

object GenericInteractions:
  def interactWith[T <: GenericContainer[T]](
      c: T,
      containerType: String
  ) =
    ZIO.blocking(ZIO.succeed(c.start)) *>
      ZIO.debug(
        s"Finished blocking during $containerType container creation"
      )

  def manage[T <: GenericContainer[T]](
      c: T,
      containerType: String
  ) =
    ZManaged.acquireReleaseWith(
      ZIO.debug(s"Creating $containerType") *>
        interactWith(c, containerType) *>
        ZIO.succeed(c)
    )((n: T) =>
      ZIO.attempt(n.close()).orDie *>
        ZIO.debug(s"Closing $containerType")
    )

  def manageWithInitialization[
      T <: GenericContainer[T]
  ](
      c: T,
      containerType: String,
      initialize: T => ZIO[Any, Throwable, Unit]
  ) =
    ZManaged.acquireReleaseWith(
      ZIO.debug(s"Creating $containerType") *>
        interactWith(c, containerType) *>
        initialize(c) *> ZIO.succeed(c)
    )((n: T) =>
      ZIO.attempt(n.close()).orDie *>
        ZIO.debug(s"Closing $containerType")
    )
end GenericInteractions

object KafkaContainerZ:
  def apply(network: Network): KafkaContainer =
    new KafkaContainer(
      DockerImageName
        .parse("confluentinc/cp-kafka:5.4.3")
        .nn
    ).nn

  def construct(): ZLayer[Has[
    Network
  ], Throwable, Has[KafkaContainer]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      container = apply(network)
      // _ <- container.getBootstrapServers
      res <-
        GenericInteractions
          .manageWithInitialization(container, "kafka", KafkaInitialization.initialize)
          .toLayer
    yield res
end KafkaContainerZ

object KafkaInitialization:
  import zio.durationInt
  val topicName = "person_events"
  def initialize(kafkaContainer: KafkaContainer): ZIO[Any, Throwable, Unit] = for {
    container <-
      ZIO.attempt {
      println("Initializing kafka...")
      val properties = new java.util.Properties()

      import org.apache.kafka.clients.admin.AdminClientConfig
      import org.apache.kafka.clients.admin.Admin
      properties.put(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()
      );
      val partitions = 1
      val replicationFactor: Short = 1
      val newTopic = new NewTopic(topicName, partitions, replicationFactor);

      val admin = Admin.create(properties).nn
      import scala.jdk.CollectionConverters._
      admin.createTopics(List(newTopic).asJava).nn
    }
    submittedMsgMetaData <- UseKafka.submitMessage("Content!", topicName, kafkaContainer)
    fiber <- UseKafka.consumeMessage(topicName, kafkaContainer).fork
    _ <- ZIO.debug("submittedMsgMetaData: " + submittedMsgMetaData)
    _ <- fiber.join
    // _ <- ZIO.sleep(2.seconds)
    _ <- UseKafka.consumeMessage(topicName, kafkaContainer)
  } yield ()
object UseKafka:
  import scala.jdk.CollectionConverters._
  def submitMessage(content: String, topicName: String, kafkaContainer: KafkaContainer) = {
    val config = new java.util.Properties().nn
    config.put("client.id", InetAddress.getLocalHost().nn.getHostName().nn)
    config.put("bootstrap.servers", kafkaContainer.getBootstrapServers.nn)
    config.put("acks", "all")
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    val producer = new KafkaProducer[String, String](config)
    val partition = 0 
    val timestamp = Instant.now().nn.toEpochMilli
    val key = "keyX"
    val value = content
    import org.apache.kafka.common.header.Header
    val  headers: List[Header] = List.empty

    
    ZIO.fromFutureJava(
      producer.send(new ProducerRecord(topicName, partition, timestamp, key, value, headers.asJava)).nn
    )
  }

  def consumeMessage(topicName: String, kafkaContainer: KafkaContainer) = ZIO.blocking{

    ZIO.debug("About to consume") *>
    ZIO.attempt {
      val config = new java.util.Properties().nn
      config.put("client.id", InetAddress.getLocalHost().nn.getHostName().nn);
      config.put("group.id", "foo");
      config.put("bootstrap.servers", kafkaContainer.getBootstrapServers.nn)
      config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      val consumer = new KafkaConsumer[String, String](config)
      consumer.subscribe(List(topicName).asJava)
      consumer.seekToBeginning(List(new TopicPartition(topicName, 1).nn).asJava)
      import java.time.Duration
      // consumer.seek(new TopicPartition(topicName, 0).nn)
      val records: ConsumerRecords[String, String]  = consumer.poll(Duration.ofSeconds(15).nn).nn
      records.forEach { record => println("Consumed record: " + record.nn.value)}
      consumer.close
    }
  }