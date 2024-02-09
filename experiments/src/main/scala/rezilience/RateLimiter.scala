package rezilience

import nl.vroste.rezilience.RateLimiter

/** This is useful for scenarios such as:
  *   - Making sure you don't suddenly spike your
  *     AWS bill
  *   - Not accidentally DDOSing a service
  */
val makeRateLimiter =
  RateLimiter.make(max = 1, interval = 1.second)

// We use Throwable as error type in this example
val expensiveApiCall =
  ZIO.unit

import zio_helpers.timedSecondsDebug
object RateLimiterDemoWithLogging
    extends ZIOAppDefault:

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(expensiveApiCall)
        // Print the time to generate each key:
        .timedSecondsDebug("Called API")
        // Repeat as fast as the limiter allows:
        .repeatN(3)
        // Print the last result
        .timedSecondsDebug("Result").run

object RateLimiterDemoGlobal
    extends ZIOAppDefault:

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      ZIO
        .foreachPar(List("Bill", "Bruce", "James")): name =>
          rateLimiter:
            expensiveApiCall
          .timedSecondsDebug(
            s"$name called API"
          )
          // Repeats as fast as allowed
          .repeatN(2)
        .unit
        .timedSecondsDebug("Total time")
        .run
end RateLimiterDemoGlobal
