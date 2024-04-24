package testcontainers

import scala.jdk.CollectionConverters.*

object RecordManipulation:
  import org.apache.kafka.clients.consumer.ConsumerRecord
  def getField(
      fieldName: String,
      record: ConsumerRecord[String, String]
  ) =
    val fields: List[String] =
      java
        .util
        .Arrays
        .asList(record.value.split(","))
        .asScala
        .last
        .toList

    val field: String =
      fields
        .find(_.startsWith(fieldName))
        .getOrElse(
          throw new IllegalArgumentException(
            s"Bad fieldName : $fieldName"
          )
        )

    field.dropWhile(_ != ':').drop(1)

  end getField
end RecordManipulation
