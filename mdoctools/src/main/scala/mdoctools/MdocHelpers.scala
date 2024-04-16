package mdoctools

import zio.Runtime.default.unsafe

extension [R, E, A](z: ZLayer[R, E, A])
  def tapWithMessage(
      message: String
  ): ZLayer[R, E, A] =
    z.tap(
      _ => ZIO.succeed(println(message))
    )
//    z.zipParLeft(background.forkDaemon)

// Consider crashing if output is unexpectedly long
def runDemo[E, A](z: => ZIO[Scope, E, A]): Unit =
  Unsafe.unsafe {
    (u: Unsafe) =>
      given Unsafe =
        u
      val res =
        unsafe
          .run(
            Rendering
              .renderEveryPossibleOutcomeZio(
                z.provide(Scope.default)
              )
              .withConsole(OurConsole)
          )
          .getOrThrowFiberFailure()
      // This is the *only* place we can trust to
      // always print the final value
      println("Result: " + res)
  }

import zio.System
import zio.test.*

def runSpec[E](
    x: ZIO[Scope, E, TestResult],
    aspects: TestAspect[Scope, Scope, E, E]*
) =

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
        OurConsole, // TODO Figure out why this doesn't work
        System.SystemLive,
        Random.RandomLive
      )
    )
  end liveEnvironment

  val annotatedSpec =
    aspects
      .foldLeft(zio.test.test("")(x))(_ @@ _)

  runDemo(
    ZioTestExecution
      .runSpecAsApp(annotatedSpec)
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
  )
end runSpec

trait ToRun:
  val bootstrap: ZLayer[Any, Nothing, Any] = ZLayer.empty
  def run: ZIO[Any, Any, Any]

  def getOrThrowFiberFailure(): Unit =
    Unsafe.unsafe { implicit unsafe =>
      val e = mdoctools.Rendering.renderEveryPossibleOutcomeZio(run)
      val result = Runtime.unsafe.fromLayer(bootstrap).unsafe.run(e).getOrThrowFiberFailure()
      println(s"Result: $result")
    }