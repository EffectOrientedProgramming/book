## experiments-src-test-scala-random

 

### experiments/src/test/scala/random/RandomRosencrantsSpec.scala
```scala
package random

import zio.test.*

object RandomRosencrantsSpec
    extends ZIOSpecDefault:
  def spec =
    suite("random")(
      test(
        "rosencrants and guildenstern are dead"
      ):
        defer {
          val coinToss =
            defer:
              if (Random.nextBoolean.run)
                ZIO
                  .debug:
                    "R: Heads."
                  .run
              else
                ZIO
                  .fail:
                    "Tails encountered. Ending performance."
                  .run

          TestRandom
            .feedBooleans(Seq.fill(100)(true)*)
            .run
          ZIO.debug:
            "*Performance Begins*"
          .run
          coinToss.repeatN(4).run

          ZIO
            .debug:
              "G: There is an art to building suspense."
            .run
          coinToss.run
          ZIO
            .debug:
              "G: Though it can be done by luck alone."
            .run
          coinToss.run
          ZIO
            .debug:
              "G: ...probability"
            .run
          coinToss.run
          assertCompletes
        }
    )

end RandomRosencrantsSpec

// G: There is an art to building suspense.
```


### experiments/src/test/scala/random/RunEffectfulGuessingGameSpec.scala
```scala
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

```


