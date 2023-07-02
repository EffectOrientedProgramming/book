package rezilience

import zio._
import zio.direct._
import nl.vroste.rezilience._

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
  Random.nextInt

object RateLimiterDemo extends ZIOAppDefault:
  def run =
    defer {
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        .repeatN(
          5
        ) // This will repeat as fast as the limiter allows
        .debug("Result").run
    }

object RateLimiterDemoWithLogging
    extends ZIOAppDefault:
  def run =
    defer {
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        .timed // This shows the time it takes to generate each key
        .tap((duration, res) =>
          ZIO.debug(
            "Generated key: " + res + " in " +
              duration.getSeconds + " s"
          )
        )
        .map(_._2)
        .repeatN(5) // This will repeat as fast as the limiter allows
        .timed
        .map((duration, res) =>
          "Result: " + res + " in " +
            duration.getSeconds + " s"
        )
        .debug
        .run
    }
end RateLimiterDemoWithLogging
