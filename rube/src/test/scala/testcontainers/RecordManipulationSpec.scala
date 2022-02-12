package testcontainers

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.apache.kafka.clients.consumer.ConsumerRecord

// TODO Figure out fi
// TESTCONTAINERS_RYUK_DISABLED=true is a
// band-aid that's avoiding the real problem with
// test cleanup

object RecordManipulationSpec
    extends DefaultRunnableSpec:

  import zio.durationInt

  def spec =
    suite("mdoc.MdocHelperSpec")(
      test("stream approach") {
        val record =
          new ConsumerRecord[String, String](
            "topic",
            0,
            0,
            "key",
            "Job:Janitor,Location:USA"
          )
        println(
          RecordManipulation
            .getField("Location", record)
        )
        for res <- ZIO.succeed(1)
        yield assert(res)(equalTo(1))
      }
    )
end RecordManipulationSpec
