package scenarios

import zio.test.*
import zio.Console.printLine
import scala.concurrent.TimeoutException

object SecuritySystemSpec extends ZIOSpecDefault:
  def spec =
    suite("SecuritySystemSpec")(

      suite("Module pattern version")(
        test("runs out of data")(
          defer {
            val system = ZIO.service[SecuritySystemX].run
            val res =
              system
                .shouldAlertServices()
                .catchSome {
                  case _: TimeoutException =>
                    printLine(
                      "Invalid Scenario. Ran out of sensor data."
                    )
                }
                .run

            ZIO.debug("Final result: " + res).run
            assertCompletes
          }
        ).provide(SecuritySystemX.live,
          MotionDetector.live ++
            AcousticDetectorX(
              (2.seconds, Decibels(11)),
              (1.seconds, Decibels(20))
            )
        ) @@ TestAspect.withLiveClock @@
          TestAspect.tag("important", "slow") @@
          TestAspect.flaky @@
          TestAspect.silent @@ TestAspect.timed
      )
    )
end SecuritySystemSpec
