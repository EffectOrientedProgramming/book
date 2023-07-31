package random

import zio.test.*

import zio.internal.stacktracer.SourceLocation

def test[In](label: String)(
  assertion: => In
)(implicit
  testConstructor: TestConstructor[Nothing, In],
  sourceLocation: SourceLocation,
  trace: Trace
            ): testConstructor.Out =
  zio.test.test(label)(assertion)

import console.FakeConsole

val standaloneSpec =
  test(
    defer {
      val res =
        effectfulGuessingGame
          .withConsole(
            FakeConsole.single("3")
          )
          .provide(RandomBoundedInt.live)
          .run
      assertTrue(res == "BZZ Wrong!!")
    }
  )

object RunEffectfulGuessingGameSpec
    extends ZIOSpecDefault:
  val blah: Spec[Any, java.io.IOException] =
    test("Untestable randomness")(
      defer {
        val res =
          effectfulGuessingGame
            .withConsole(
              FakeConsole.single("3")
            )
            .provide(RandomBoundedInt.live)
            .run
        assertTrue(res == "BZZ Wrong!!")
      }
    ) @@ TestAspect.flaky
   // Highlight that we shouldn't need this TestAspect.

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
                .provide(RandomBoundedInt.live)
                .run
            assertTrue(res == "BZZ Wrong!!")
          }
        ) @@ TestAspect.flaky, // Highlight that we shouldn't need this TestAspect.
        test("Testable")(
          defer {
            val res =
              effectfulGuessingGame
                .withConsole(
                  FakeConsole.single("3")
                )
                .provide(
                  RandomBoundedIntFake(Seq(3))
                )
                .run
            assertTrue(res == "You got it!")
          }
        ) @@ TestAspect.nonFlaky(1000)
      )
    )
end RunEffectfulGuessingGameSpec
