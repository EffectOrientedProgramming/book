package scenarios

import zio.test.*
import zio.Console.printLine
import scala.concurrent.TimeoutException

object SecuritySystemSpec extends ZIOSpecDefault:
  def spec =
    suite("SecuritySystemSpec")(
      suite("shouldAlertServices")(
        test("runs out of data")(
          defer {
            val res =
              SecuritySystem
                .shouldAlertServices()
                .provide(
                  MotionDetector.live ++
                    ThermalDetectorX(
                      (1.seconds, Degrees(71)),
                      (1.seconds, Degrees(70)),
                      (3.seconds, Degrees(98))
                    ) // ++ s
                    ++
                    AcousticDetectorX(
                      (4.seconds, Decibels(11)),
                      (1.seconds, Decibels(20))
                    ) ++ SirenX.live
                )
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
        ) @@ TestAspect.withLiveClock @@
          TestAspect.tag("important", "slow") @@
          TestAspect.flaky @@
          TestAspect.silent @@ TestAspect.timed
      )
    )
end SecuritySystemSpec
