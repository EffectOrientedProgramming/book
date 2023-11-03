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
