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
      .attemptBlocking(leven(input, target))
      .mapBoth(
        error => ZIO.succeed("yay!"),
        success => ZIO.fail("Oh no!")
      )

  def run =
    // For timeouts, you need fibers and
    // cancellation
    scenario
      .timeout(50.seconds)
      .timed
      .debug("Time:")
