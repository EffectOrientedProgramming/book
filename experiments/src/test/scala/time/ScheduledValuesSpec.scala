package time

import zio.*
import zio.test.*

import java.time.Instant
import scala.concurrent.TimeoutException

object ScheduledValuesSpec extends ZIOSpecDefault:
  def spec =
    suite("ScheduledValues")(
      suite("scheduledValues")(
        test("querying after no time has passed returns the first value, if duration was > 0")(
          for
            valueAccessor <- scheduledValues(
              (1.seconds, "First Section"),
            )
            firstValue <- valueAccessor
          yield assertTrue(firstValue == "First Section")
        ),
        test("querying after no time has passed fails when the duration == 0")(
          for
            valueAccessor <- scheduledValues(
              (0.seconds, "First Section"),
            )
            _ <- valueAccessor.flip
          yield assertCompletes
        ),
        test("next value is returned after enough time has elapsed")(
          for
            valueAccessor <- scheduledValues(
              (1.seconds, "First Section"),
              (2.seconds, "Second Section"),
            )
            _ <- TestClock.adjust(1.seconds)
            secondValue <- valueAccessor
          yield
            assertTrue(secondValue == "Second Section")
        ),
        test("time range end is not inclusive")(
          for
            valueAccessor <- scheduledValues(
              (1.seconds, "First Section"),
            )
            _ <- TestClock.adjust(1.seconds)
            _ <- valueAccessor.flip
          yield
            assertCompletes
        )
      )
    )
