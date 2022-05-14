package testcontainers

import zio.*
import zio.Console.printLine
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
import zio.stream.ZStream
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.jdk.CollectionConverters._
import zio.ZLayer

object KafkaContainerZ:
  def apply(network: Network): KafkaContainer =
    new KafkaContainer(
      DockerImageName
        .parse("confluentinc/cp-kafka:5.4.3")
        .nn
    ).nn

  def construct(
      topicNames: List[String]
  ): ZLayer[
    Network & NetworkAwareness,
    Throwable,
    KafkaContainer
  ] = ???
/* TODO Restore once the rest of the M6 upgrade
 * is complete for network <-
 * ZLayer.service[Network] localHostName:
 * ZEnvironment[String] <- NetworkAwareness
 * .localHostName .toLayer container =
 * apply(network.get) res:
 * ZEnvironment[KafkaContainer] <-
 * GenericInteractionsZ
 * .manageWithInitialization( container, "kafka",
 * KafkaInitialization.initialize( _,
 * localHostName.get, topicNames ) ) .toLayer
 * yield res */
end KafkaContainerZ

object KafkaInitialization:
  import zio.durationInt
  def initialize(
      kafkaContainer: KafkaContainer,
      localHostname: String,
      topicNames: List[String]
  ): ZIO[Any, Throwable, Unit] =
    for container <-
        ZIO.attempt {
          val properties =
            new java.util.Properties

          import org.apache.kafka.clients.admin.AdminClientConfig
          import org.apache.kafka.clients.admin.Admin
          properties.put(
            AdminClientConfig
              .BOOTSTRAP_SERVERS_CONFIG,
            kafkaContainer.getBootstrapServers()
          );
          properties
            .put("client.id", localHostname)
          val partitions               = 1
          val replicationFactor: Short = 1
          val newTopics =
            topicNames.map(topicName =>
              new NewTopic(
                topicName,
                partitions,
                replicationFactor
              )
            )

          val admin = Admin.create(properties).nn
          import scala.jdk.CollectionConverters._
          admin.createTopics(newTopics.asJava).nn
        }
    yield ()
end KafkaInitialization

case class KafkaProducerZ(
    rawProducer: KafkaProducer[String, String],
    topicName: String,
    messagesProduced: Ref[Int]
):
  import scala.jdk.CollectionConverters._
  def submit(
      key: String,
      value: String
  ): Task[RecordMetadata] =
    val partition = 0
    val timestamp = Instant.now().nn.toEpochMilli
    import org.apache.kafka.common.header.Header
    val headers: List[Header] = List.empty

    messagesProduced.update(_ + 1) *>
      ZIO.fromFutureJava(
        rawProducer
          .send(
            new ProducerRecord(
              topicName,
              partition,
              timestamp,
              key,
              value,
              headers.asJava
            )
          )
          .nn
      )
  end submit

  def submitForever(
      key: String,
      value: String
  ): ZIO[Clock & Console, Throwable, Unit] =
    for
      curMessagesProduced <- messagesProduced.get
      _ <-
        printLine(
          "submitting record for: " + key
        )
      _ <-
        submit(key, value + curMessagesProduced)
      _ <- messagesProduced.update(_ + 1)
      _ <-
      ZIO.sleep(1.second) *>
        submitForever(key, value)
    yield ()
    end for
  end submitForever
end KafkaProducerZ

case class KafkaConsumerZ(
    rawConsumer: KafkaConsumer[String, String],
    topicName: String,
    messagesConsumed: Ref[Int]
):
  // TODO Handle closing underlying consumer
  import java.time.Duration

  def poll(): Task[
    List[ConsumerRecord[String, String]]
  ] =
    for
      newRecords <-
        ZIO.attempt {
          val records
              : ConsumerRecords[String, String] =
            rawConsumer
              .poll(Duration.ofMillis(100).nn)
              .nn
          rawConsumer.commitSync
          records
            .records(topicName)
            .nn
            .asScala
            .toList // TODO Parameterize/access topicName more cleanly
        }
      _ <-
        messagesConsumed
          .update(_ + newRecords.length)
    yield newRecords

  def pollStream(): ZStream[Any, Throwable, List[
    ConsumerRecord[String, String]
  ]] =
    val debug = false
    ZStream
      .repeatZIO(poll())
      .tap(recordsConsumed =>
        // Should this stay as debug?
        if debug then
          ZIO.foreach(
            recordsConsumed.map { record =>
              record.nn.value.toString
            }
          )(record =>
            ZIO.debug(
              s"Consumed $topicName record: $record"
            )
          )
        else
          ZIO.unit
      )
  end pollStream

end KafkaConsumerZ

object UseKafka:

  def createProducer(topicName: String): ZIO[
    KafkaContainer,
    Throwable,
    KafkaProducerZ
  ] =
    for
      kafkaContainer <-
        ZIO.service[KafkaContainer]
      messagesProduced <- Ref.make(1)
    yield
      val config = new java.util.Properties.nn
      config.put(
        "client.id",
        InetAddress
          .getLocalHost()
          .nn
          .getHostName()
          .nn
      )
      config.put(
        "bootstrap.servers",
        kafkaContainer.getBootstrapServers.nn
      )
      config.put("acks", "all")
      config.put(
        "key.serializer",
        "org.apache.kafka.common.serialization.StringSerializer"
      )
      config.put(
        "value.serializer",
        "org.apache.kafka.common.serialization.StringSerializer"
      )
      KafkaProducerZ(
        new KafkaProducer[String, String](
          config
        ),
        topicName,
        messagesProduced
      )

  def createConsumer(
      topicName: String,
      groupId: String
  ): ZIO[
    KafkaContainer,
    Throwable,
    KafkaConsumerZ
  ] =
    for
      kafkaContainer <-
        ZIO.service[KafkaContainer]
      messagesConsumed <- Ref.make(0)
    yield
      val config = new java.util.Properties.nn
      config.put(
        "client.id",
        InetAddress
          .getLocalHost()
          .nn
          .getHostName()
          .nn
      );
      config.put("group.id", groupId);
      config.put(
        "bootstrap.servers",
        kafkaContainer.getBootstrapServers.nn
      )
      // config.put("max.poll.records", "1")
      config.put("auto_offset_rest", "earliest")

      config.put("enable.auto.commit", "true");
      config
        .put("auto.commit.interval.ms", "500");
      config.put("session.timeout.ms", "30000");
      config
        .put("enable.partition.eof", "false");
      config.put(
        "key.deserializer",
        "org.apache.kafka.common.serialization.StringDeserializer"
      )
      config.put(
        "value.deserializer",
        "org.apache.kafka.common.serialization.StringDeserializer"
      )
      val consumer =
        new KafkaConsumer[String, String](config)
      consumer.subscribe(List(topicName).asJava)
      // consumer.seekToBeginning(List(new
      // TopicPartition(topicName, 1).nn).asJava)
      KafkaConsumerZ(
        consumer,
        topicName,
        messagesConsumed
      )

  // TODO Decide whether to delete this
  def createForwardedStream[R, E](
      topicName: String,
      op: ConsumerRecord[String, String] => ZIO[
        R,
        E,
        String
      ],
      // TODO Can we create the Producer inside
      // here?
      output: KafkaProducerZ,
      groupId: String
  ): ZIO[
    Console & R & KafkaContainer,
    Throwable | E,
    Unit
  ] =
    createConsumer(topicName, groupId).flatMap(consumer =>
      consumer
        .pollStream()
        .foreach(recordsConsumed =>
          ZIO.foreach(recordsConsumed)(record =>
            for
              newValue <- op(record)
              _ <-
                printLine(
                  s"${consumer.topicName} --> ${record
                      .value} => $newValue--> ${output.topicName}"
                )
              _ <-
                output.submit(
                  record.key.nn,
                  newValue
                )
            yield ()
          )
        )
    )

  def createForwardedStreamZ[R, E](
      topicName: String,
      op: ConsumerRecord[String, String] => ZIO[
        R,
        E,
        String
      ],
      // TODO Can we create the Producer inside
      // here?
      outputTopicName: String,
      groupId: String
  ): ZIO[
    Console & R & KafkaContainer,
    Any,
    Unit
  ] = // TODO Narrow error type
    for
      producer <-
        UseKafka.createProducer(outputTopicName)
      _ <-
        createConsumer(topicName, groupId)
          .flatMap(consumer =>
            consumer
              .pollStream()
              .foreach(recordsConsumed =>
                ZIO.foreach(recordsConsumed)(
                  record =>
                    for
                      newValue <- op(record)
                      _ <-
                        printLine(
                          s"${consumer.topicName} --> ${record.value} => $newValue--> ${outputTopicName}"
                        )
                      _ <-
                        producer.submit(
                          record.key.nn,
                          newValue
                        )
                    yield ()
                )
              )
          )
    yield ()

  def createSink[R, E](
      topicName: String,
      op: ConsumerRecord[String, String] => ZIO[
        R,
        E,
        Unit
      ],
      groupId: String
  ): ZIO[
    Console & R & KafkaContainer,
    Any,
    Unit
  ] = // TODO Narrow error type
    createConsumer(topicName, groupId)
      .flatMap(consumer =>
        consumer
          .pollStream()
          .foreach(recordsConsumed =>
            ZIO
              .foreach(recordsConsumed)(record =>
                for
                  newValue <- op(record)
                  _ <-
                    printLine(
                      s"Terminal Consumption: ${consumer.topicName} --> ${record.value}"
                    )
                yield ()
              )
          )
      )
end UseKafka
