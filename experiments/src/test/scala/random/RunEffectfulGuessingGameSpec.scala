package random

import zio.test.*
import zio.direct.*
import console.FakeConsole
import zio.*

object RunEffectfulGuessingGameSpec
    extends ZIOSpecDefault:
  def spec =
    suite("GuessingGame")(
      suite("Effectful")(
        test("Untestable randomness")(
          defer {
            effectfulGuessingGame
              .withConsole(FakeConsole.single("3"))
              .provide(
                RandomBoundedInt.live,
              )
              .run
            assertCompletes
          }
        ),
        test("Testable")(
          defer {
            effectfulGuessingGame
              .withConsole(FakeConsole.single("3"))
              .provide(
                RandomBoundedIntFake(Seq(3))
              )
              .run
            assertCompletes
          }
        )
      )
    )
