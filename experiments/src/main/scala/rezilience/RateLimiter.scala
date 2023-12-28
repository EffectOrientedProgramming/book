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
  Random.nextInt

object RateLimiterDemo extends ZIOAppDefault:
  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        // Repeats as fast as the limiter allows
        .repeatN(5).debug("Result").run

object RateLimiterDemoWithLogging
    extends ZIOAppDefault:

  // TODO Put in book-side ZIO helpers?
  extension [R, E, A](z: ZIO[R, E, A])
    def timedSecondsDebug(
        message: String
    ): ZIO[R, E, A] =
      z.timed
        .tap: (duration, res) =>
          ZIO.debug:
            message + ": " + res + " [took " +
              duration.getSeconds + "s]"
        .map(_._2)

  def run =
    defer:
      val rateLimiter = makeRateLimiter.run
      rateLimiter(rsaKeyGenerator)
        // Print the time to generate each key:
        .timedSecondsDebug("Generated key")
        // Repeat as fast as the limiter allows:
        .repeatN(5)
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
        .repeatNPar(4): i =>
          rateLimiter(
            rsaKeyGenerator.debug(i.toString)
          )
            // Repeats as fast as allowed
            .repeatN(5).debug(s"Result $i")
        .run
end RateLimiterDemoGlobal
