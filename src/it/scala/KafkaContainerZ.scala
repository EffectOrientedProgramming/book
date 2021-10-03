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
        start(c, containerType) *>
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
      initialize: T => ZIO[Any, Throwable, Unit] = (_: T) => ZIO.unit
  ) =
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

object KafkaContainerZ:
  def apply(network: Network): KafkaContainer =
    new KafkaContainer(
      DockerImageName
        .parse("confluentinc/cp-kafka:5.4.3")
        .nn
    ).nn

  def construct(): ZLayer[Has[
    Network
  ] & Has[NetworkAwareness], Throwable, Has[KafkaContainer]] =
    for
      network <-
        ZLayer.service[Network].map(_.get)
      localHostName <- NetworkAwareness.localHostName.toLayer.map(_.get)
      container = apply(network)
      // _ <- container.getBootstrapServers
      res <-
        GenericInteractions
          .manageWithInitialization(container, "kafka", KafkaInitialization.initialize(_, localHostName))
          .toLayer
    yield res
end KafkaContainerZ

object KafkaInitialization:
  import zio.durationInt
  val topicName = "person_events"
  def initialize(kafkaContainer: KafkaContainer, localHostname: String): ZIO[Any, Throwable, Unit] = for {
    container <-
      ZIO.attempt {
      val properties = new java.util.Properties()

      import org.apache.kafka.clients.admin.AdminClientConfig
      import org.apache.kafka.clients.admin.Admin
      properties.put(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()
      );
      properties.put("client.id", localHostname)
      val partitions = 1
      val replicationFactor: Short = 1
      val newTopic = new NewTopic(topicName, partitions, replicationFactor);

      val admin = Admin.create(properties).nn
      import scala.jdk.CollectionConverters._
      admin.createTopics(List(newTopic).asJava).nn
    }
  } yield ()

class KafkaProducerZ(rawProducer: KafkaProducer[String, String]):
  import scala.jdk.CollectionConverters._
  def submit(key: String, value: String, topicName: String): Task[RecordMetadata] = {
    val partition = 0 
    val timestamp = Instant.now().nn.toEpochMilli
    import org.apache.kafka.common.header.Header
    val  headers: List[Header] = List.empty
    
    ZIO.fromFutureJava(
      rawProducer.send(new ProducerRecord(topicName, partition, timestamp, key, value, headers.asJava)).nn
    )
  }

  private var numberOfSubmissions = 0
  private val maxSubmissions = 10

  def submitForever(key: String, value: String, topicName: String): Task[Unit] = {
    val partition = 0 
    val timestamp = Instant.now().nn.toEpochMilli
    import org.apache.kafka.common.header.Header
    val  headers: List[Header] = List.empty
    
    ZIO.fromFutureJava(
      rawProducer.send(new ProducerRecord(topicName, partition, timestamp, key + numberOfSubmissions, value + numberOfSubmissions, headers.asJava)).nn
    )
  } *> 
  ( if numberOfSubmissions < maxSubmissions then
      numberOfSubmissions = numberOfSubmissions + 1
      ZIO.blocking { ZIO.attempt{ Thread.sleep(1000)} } *> submitForever(key, value, topicName)
    else 
      ZIO.unit)

class KafkaConsumerZ(rawConsumer: KafkaConsumer[String, String]):
  // TODO Handle closing underlying consumer
  def poll() =  ZIO.attempt{
      import java.time.Duration
      // consumer.seek(new TopicPartition(topicName, 0).nn)
      for (i <- Range(1, 5)) {
        println("Consuming loop: " + i)
        val records: ConsumerRecords[String, String]  = rawConsumer.poll(Duration.ofSeconds(1).nn).nn
        records.forEach { record => println("Consumed record: " + record.nn.value)}
      }
  }

  private var numberOfPolls = 0
  private val maxPolls = 10
  def pollForever(): ZIO[Any, Throwable, Unit] =  ZIO.attempt{
      import java.time.Duration
      // consumer.seek(new TopicPartition(topicName, 0).nn)
      println("Polling forever")
      val records: ConsumerRecords[String, String]  = rawConsumer.poll(Duration.ofSeconds(1).nn).nn
      records.forEach { record => println("Consumed record: " + record.nn.value)}
  } *> 
    (if numberOfPolls < maxPolls then
      numberOfPolls = numberOfPolls + 1
      pollForever()
    else
      ZIO.unit)
end KafkaConsumerZ

object UseKafka:
  import scala.jdk.CollectionConverters._

  def createProducer(): ZIO[Has[KafkaContainer], Nothing, KafkaProducerZ] = 
    for {
      kafkaContainer <- ZIO.service[KafkaContainer]
    } yield {
        val config = new java.util.Properties().nn
        config.put("client.id", InetAddress.getLocalHost().nn.getHostName().nn)
        config.put("bootstrap.servers", kafkaContainer.getBootstrapServers.nn)
        config.put("acks", "all")
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        KafkaProducerZ(new KafkaProducer[String, String](config))
    }

  def createConsumer(topicName: String) = 
    for {
      kafkaContainer <- ZIO.service[KafkaContainer]
    } yield
    {
      val config = new java.util.Properties().nn
      config.put("client.id", InetAddress.getLocalHost().nn.getHostName().nn);
      config.put("group.id", "foo");
      config.put("bootstrap.servers", kafkaContainer.getBootstrapServers.nn)
      println("BootstopServers: " + kafkaContainer.getBootstrapServers.nn)
      config.put("max.poll.records", "1")
      config.put("auto_offset_rest","earliest")
      
      config.put("enable.auto.commit", "true");
      config.put("auto.commit.interval.ms", "500");
      config.put("session.timeout.ms", "30000");
      config.put("enable.partition.eof", "false");
      config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      val consumer = new KafkaConsumer[String, String](config)
      consumer.subscribe(List(topicName).asJava)
      // consumer.seekToBeginning(List(new TopicPartition(topicName, 1).nn).asJava)
      KafkaConsumerZ(consumer)
  }
