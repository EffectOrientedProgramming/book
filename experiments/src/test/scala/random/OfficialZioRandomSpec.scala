package random

import zio.test.*

object OfficialZioRandomSpec extends ZIOSpecDefault {
  def spec =
    suite("random")(
      test("hello random")(
        defer {
          // Note: Once the test exhausts these values, it goes
          // back to true random values.
          TestRandom.feedInts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).run
          val result = Random.nextIntBounded(1000).run
          assertTrue(result == 1)
          val result2 = Random.nextIntBounded(1000).run
          assertTrue(result2 == 2)

        }
      ),
      test("rosencrants and guildenstern are dead")(
        defer {
          val coinToss = defer {
            if (Random.nextBoolean.run)
              ZIO.debug("ROSENCRANTZ: Heads.").run
            else
              ZIO.fail("Tails encountered. Ending performance.").run
          }
          TestRandom.feedBooleans(Seq.fill(100)(true)*).run
          coinToss.repeatN(4).run

          ZIO.debug("GUILDENSTERN: There is an art to the building up of suspense.").run
          coinToss.run
          ZIO.debug("GUILDENSTERN: Though it can be done by luck alone.").run
          coinToss.run
          assertCompletes

        }
      )
    )

}
