## experiments-src-test-scala-random

 

### experiments/src/test/scala/random/OfficialZioRandomSpec.scala
```scala
package random

import zio.test.*

object OfficialZioRandomSpec
    extends ZIOSpecDefault:
  def spec =
    suite("random")(
      test("hello random")(
        defer {
          // Note: Once the test exhausts these
          // values, it goes
          // back to true random values.
          TestRandom
            .feedInts(
              1, 2, 3, 4, 5, 6, 7, 8, 9, 10
            )
            .run
          val result =
            Random.nextIntBounded(1000).run
          assertTrue(result == 1)
          val result2 =
            Random.nextIntBounded(1000).run
          assertTrue(result2 == 2)

        }
      ),
      test(
        "rosencrants and guildenstern are dead"
      )(
        defer {
          val coinToss =
            defer {
              if (Random.nextBoolean.run)
                ZIO
                  .debug("ROSENCRANTZ: Heads.")
                  .run
              else
                ZIO
                  .fail(
                    "Tails encountered. Ending performance."
                  )
                  .run
            }
          TestRandom
            .feedBooleans(Seq.fill(100)(true)*)
            .run
          coinToss.repeatN(4).run

          ZIO
            .debug(
              "GUILDENSTERN: There is an art to the building up of suspense."
            )
            .run
          coinToss.run
          ZIO
            .debug(
              "GUILDENSTERN: Though it can be done by luck alone."
            )
            .run
          coinToss.run
          assertCompletes

        }
      ),
      test("asdf")(
        defer:
          TestRandom.feedInts(1, 2).run
          val result1 = Random.nextInt.run
          val result2 = Random.nextInt.run
          //val result3 = Random.nextInt.run // this falls back to system Random
          assertTrue(
            result1 == 1,
            result2 == 2,
            //result3 == 5
          )
      ),
      test("timeout"):
        val thingThatTakesTime =
          ZIO.sleep(2.seconds)

        defer:
          val fork =
            thingThatTakesTime
              .timeout(1.second)
              .fork
              .run
          TestClock.adjust(2.seconds).run
          val result = fork.join.run
          assertTrue(result.isEmpty)
      ,
      //test("failure"):
        //assertTrue(Some("asdf") == None)
    )

end OfficialZioRandomSpec

```


### experiments/src/test/scala/random/RunEffectfulGuessingGameSpec.scala
```scala
package random

import zio.test.*

import zio.internal.stacktracer.SourceLocation
import console.FakeConsole

/* def test[In](label: String)( assertion: => In
 * )(implicit testConstructor:
 * TestConstructor[Nothing, In], sourceLocation:
 * SourceLocation, trace: Trace ):
 * testConstructor.Out =
 * zio.test.test(label)(assertion)
 *
 * val standaloneSpec =
 * test( defer { val res =
 * effectfulGuessingGame .withConsole(
 * FakeConsole.single("3") )
 * .provide(RandomBoundedInt.live) .run
 * assertTrue(res == "BZZ Wrong!!") } ) */

object RunEffectfulGuessingGameSpec
    extends ZIOSpecDefault:
  val blah: Spec[Any, java.io.IOException] =
    test("Untestable randomness")(
      defer {
        val res =
          effectfulGuessingGame
            .withConsole(FakeConsole.single("3"))
            .provide(RandomBoundedInt.live)
            .run
        assertTrue(res == "BZZ Wrong!!")
      }
    ) @@ TestAspect.flaky
  // Highlight that we shouldn't need this
  // TestAspect.

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
        ) @@
          TestAspect
            .flaky, // Highlight that we shouldn't need this TestAspect.
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
        ) @@ TestAspect.nonFlaky(10)
      )
    )
end RunEffectfulGuessingGameSpec

```


