package scenarios

import zio.*
import zio.test.*
import zio.Console.printLine
import scala.concurrent.TimeoutException

object SecuritySystemSpec
    extends ZIOSpecDefault:
  def spec = suite("SecuritySystemSpec")(
    suite("shouldAlertServices")(
      test("runs out of data") (
        for
          res <- SecuritySystem
            .shouldAlertServices()
            .provide(
              SecuritySystem.fullServiceBuilder
            )
            .catchSome {
              case _: TimeoutException =>
                printLine(
                  "Invalid Scenario. Ran out of sensor data."
                )
            }

          _ <- ZIO.debug("Final result: " + res)
        yield assertCompletes
      ) @@ TestAspect.withLiveClock
    )
  )
