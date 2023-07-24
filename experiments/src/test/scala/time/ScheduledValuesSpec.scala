package time

import zio.test.*

import scala.concurrent.TimeoutException

object ScheduledValuesSpec
    extends ZIOSpecDefault:
  def spec =
    suite("ScheduledValues")(
      suite("scheduledValues")(
        test(
          "querying after no time has passed returns the first value, if duration was > 0"
        )(
          defer {
            val valueAccessor =
              scheduledValues(
                (1.seconds, "First Section")
              ).run
            assertTrue(
              valueAccessor.run ==
                "First Section"
            )
          }
        ),
        test(
          "querying after no time has passed fails when the duration == 0"
        )(
          defer {
            val valueAccessor =
              scheduledValues(
                (0.seconds, "First Section")
              ).run
            valueAccessor.flip.run
            assertCompletes
          }
        ),
        test(
          "next value is returned after enough time has elapsed"
        )(
          defer {
            val valueAccessor =
              scheduledValues(
                (1.seconds, "First Section"),
                (2.seconds, "Second Section")
              ).run
            TestClock.adjust(1.seconds).run
            val secondValue = valueAccessor.run
            assertTrue(
              secondValue == "Second Section"
            )
          }
        ),
        test("time range end is not inclusive")(
          defer {
            val valueAccessor =
              scheduledValues(
                (1.seconds, "First Section")
              ).run
            TestClock.adjust(1.seconds).run
            valueAccessor.flip.run
            assertCompletes
          }
        )
      )
    )
end ScheduledValuesSpec
