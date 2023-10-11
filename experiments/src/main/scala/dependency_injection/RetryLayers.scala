package dependency_injection

import zio.Runtime.default.unsafe

case class Heat()
object Heat:
  val invocations =
    Unsafe.unsafe( (u: Unsafe) =>
      given Unsafe = u
      unsafe.run( Ref.make(0))
        .getOrThrowFiberFailure()
    )

  def attempt(invocations: Ref[Int]): ZIO[Any, String, Heat] =
    invocations.updateAndGet(_ + 1).flatMap {
      case cnt if (cnt < 3) => ZIO.fail("Power is still out").debug
      case _ => ZIO.succeed(Heat()).debug("Power is back on")
    }

  // Already constructed elsewhere, that we don't control
  val spotty = ZLayer.fromZIO(
    defer:
      Heat.attempt(invocations).run
  )

object RetryLayers extends ZIOAppDefault:

  def run =
      Heat.spotty
        .retry(Schedule.recurs(3))
        .build