package fibers

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.text.similarity.LevenshteinDistance
import zio.*

val input  = RandomStringUtils.random(70_000)
val target = RandomStringUtils.random(70_000)
val leven =
  LevenshteinDistance.getDefaultInstance

object PlainLeven extends App:
  leven(input, target)

object CancellingATightLoop
    extends ZIOAppDefault:
  val scenario =
    ZIO
      .attempt(leven(input, target))
      .mapBoth(
        error => ZIO.succeed("yay!"),
        success => ZIO.fail("Oh no!")
      )

  def run =
    // For timeouts, you need fibers and
    // cancellation
    scenario
      // TODO This is running for 16 seconds
      // nomatter what.
      .timeout(1.seconds).timed.debug("Time:")
