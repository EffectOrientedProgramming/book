package random

import zio.test.*

import zio.internal.stacktracer.SourceLocation
import console.FakeConsole

object RunEffectfulGuessingGameSpec
    extends ZIOSpecDefault:

  def spec =
    suite("GuessingGame")(
      suite("Effectful")(
        test("Untestable randomness")(
          defer {
            val res =
              effectfulGuessingGame
                .withConsole(
                  FakeConsole.single("3")
                )
                .run
            assertTrue(res == "BZZ Wrong!!")
          }
        ) @@ TestAspect.withLiveRandom @@
          TestAspect
            .flaky, // Highlight that we shouldn't need this TestAspect.
        test("Testable")(
          defer {
            TestRandom.feedInts(3).run
            val res =
              effectfulGuessingGame
                .withConsole(
                  FakeConsole.single("3")
                )
//                .provide(
//                  RandomBoundedIntFake(Seq(3))
//                )
                .run
            assertTrue(res == "You got it!")
          }
        ) @@ TestAspect.nonFlaky(10)
      )
    )
end RunEffectfulGuessingGameSpec
