package random

import zio.test.*

import zio.internal.stacktracer.SourceLocation

object RunEffectfulGuessingGameSpec
    extends ZIOSpecDefault:

  def spec =
    suite("GuessingGame")(
      suite("Effectful")(
        test("Testable")(
          defer {
            TestConsole.feedLines("3").run
            TestRandom.feedInts(3).run
            val res = effectfulGuessingGame.run
            assertTrue(res == "You got it!")
          }
        ) @@ TestAspect.nonFlaky(10)
      )
    )
end RunEffectfulGuessingGameSpec
