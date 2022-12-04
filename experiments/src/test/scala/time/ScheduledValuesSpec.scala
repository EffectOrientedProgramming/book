package time

import zio.*
import zio.test.*

import java.time.Instant
import scala.concurrent.TimeoutException

object ScheduledValuesSpec extends ZIOSpecDefault:
  def spec =
    suite("ScheduledValues")(
    suite("timeTableX")(
      test("simple")(
        for
          _ <- ZIO.debug("TODO Test something!")
          timeTable <- ZIO.succeed(
            createTimeTableX(Instant.parse("2000-01-01T00:00:00.00Z"),
              (1.seconds, "First Section"),
              (2.seconds, "Second Section"),
            )
          )
        yield assertCompletes
      )

    ),
      suite("scheduledValues")(
        test("simple")(
          for
            valueAccessor <- scheduledValues(
              (1.seconds, "First Section"),
              (2.seconds, "Second Section"),
            )
            firstValue <- valueAccessor
            _ <- TestClock.adjust(1.seconds)
            secondValue <- valueAccessor
            _ <- TestClock.adjust(1.seconds)
            thirdValue <- valueAccessor
            _ <- TestClock.adjust(5.seconds)
            failure <- valueAccessor.flip
          yield assertTrue(firstValue == "First Section") &&
            assertTrue(secondValue == "Second Section") &&
            assertTrue(thirdValue == "Second Section") &&
            assertTrue(failure.getMessage == "TOO LATE") &&
            assertCompletes
        )
      )
    )
