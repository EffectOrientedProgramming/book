package dependency_injection

case class Bread()

import zio.Runtime.default.unsafe
object OtherSource:
  val invocations =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe = u
      unsafe
        .run(Ref.make(0))
        .getOrThrowFiberFailure()
    )

  val reset =
    Unsafe.unsafe((u: Unsafe) =>
      given Unsafe = u

      unsafe
        .run(invocations.set(0))
        .getOrThrowFiberFailure()
    )

  def attempt(
      invocations: Ref[Int]
  ): ZIO[Any, String, Bread] =
    invocations
      .updateAndGet(_ + 1)
      .flatMap {
        case cnt if cnt < 3 =>
          defer:
            ZIO.debug("**Power out**").run
            ZIO.fail("**Power out**").run
        case _ =>
          ZIO
            .succeed(Bread())
            .debug("Power is back on")
      }

  // Already constructed elsewhere, that we don't
  // control
  val spotty =
    ZLayer.fromZIO:
      OtherSource.attempt(invocations)
end OtherSource

object BustedLayer extends ZIOAppDefault:

  def run = OtherSource.spotty.build

object RetryLayer extends ZIOAppDefault:

  def run =
    OtherSource
      .spotty
      .retry(Schedule.recurs(3))
      .build
