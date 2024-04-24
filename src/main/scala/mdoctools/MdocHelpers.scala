package mdoctools

extension [R, E, A](z: ZLayer[R, E, A])
  def tapWithMessage(
      message: String
  ): ZLayer[R, E, A] =
    z.tap(
      _ => ZIO.succeed(println(message))
    )
//    z.zipParLeft(background.forkDaemon)

trait ToRun:
  val bootstrap: ZLayer[Any, Nothing, Any] =
    ZLayer.empty
  def run: ZIO[Scope, Any | Nothing, Any]

  def getOrThrowFiberFailure(): Unit =
    Unsafe.unsafe {
      implicit unsafe =>
        val e =
          mdoctools
            .Rendering
            .renderEveryPossibleOutcomeZio(run)
        val result =
          Runtime
            .unsafe
            .fromLayer(bootstrap)
            .unsafe
            .run(e.provide(Scope.default))
            .getOrThrowFiberFailure()
        println(s"Result: $result")
    }
end ToRun

abstract class ToTest[E, A] extends ToRun:
  def spec: Spec[TestEnvironment & Scope, Any]

  def run =
    val liveEnvironment: Layer[
      Nothing,
      Clock & Console & System & Random
    ] =
      implicit val trace =
        Trace.empty
      ZLayer.succeedEnvironment(
        ZEnvironment[
          Clock,
          Console,
          System,
          Random
        ](
          Clock
            .ClockLive, // TODO Should this be OurClock
          Console.ConsoleLive,
          System.SystemLive,
          Random.RandomLive
        )
      )
    end liveEnvironment

    ZioTestExecution
      .runSpecAsApp(spec)
      .provide(
        liveEnvironment,
        TestEnvironment.live,
        Scope.default
      )
      .map(
        result =>
          if (result.failureDetails.isBlank)
            "Test PASSED"
          else
            "Test FAILED"
      )
  end run
end ToTest
