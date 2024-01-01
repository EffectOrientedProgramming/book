package rezilience

import nl.vroste.rezilience.*

/** This is useful for scenarios such as:
  *   - Making sure you don't suddenly spike your
  *     AWS bill
  *   - Not accidentally DDOSing a service
  */
val makeRateLimiter
    : ZIO[Scope, Nothing, RateLimiter] =
  RateLimiter.make(max = 1, interval = 1.second)

// We use Throwable as error type in this example
def rsaKeyGenerator: ZIO[Any, Throwable, Int] =
  Random.nextIntBounded(1000)

import zio_helpers.timedSecondsDebug
object RateLimiterDemoWithLogging
    extends ZIOAppDefault:


  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        // Print the time to generate each key:
        .timedSecondsDebug("Generated key")
        // Repeat as fast as the limiter allows:
        .repeatN(3)
        // Print the last result
        .timedSecondsDebug("Result").run

end RateLimiterDemoWithLogging

object RateLimiterDemoGlobal
    extends ZIOAppDefault:

  import zio_helpers.repeatNPar

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      ZIO
        .repeatNPar(3): i =>
          rateLimiter:
            rsaKeyGenerator
          .timedSecondsDebug(s"${i.toString} generated a key")
            // Repeats as fast as allowed
          .repeatN(2).debug(s"Result $i")
        .unit
        .timedSecondsDebug("Total time")
        .run
end RateLimiterDemoGlobal
