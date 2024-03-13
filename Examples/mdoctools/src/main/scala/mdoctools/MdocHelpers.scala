package mdoctools

import zio.Runtime.default.unsafe

// Consider crashing if output is unexpectedly long
def runDemo[E, A](z: => ZIO[Any, E, A]): Unit =
  Unsafe.unsafe {
    (u: Unsafe) =>
      given Unsafe =
        u
      val res =
        unsafe
          .run(
            Rendering
              .renderEveryPossibleOutcomeZio(z)
              .withConsole(OurConsole)
          )
          .getOrThrowFiberFailure()
      // This is the *only* place we can trust to
      // always print the final value
      println(res)
  }

import zio.System
import zio.test.*

def runSpec[E <: Throwable](
    x: ZIO[Any, E, TestResult]
) =

  val liveEnvironment: Layer[
    Nothing,
    Clock with Console with System with Random
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

  runDemo(
    ZioTestExecution
      .runSpecAsApp(zio.test.test("")(x))
      .provide(
        liveEnvironment,
        TestEnvironment.live,
        Scope.default
      )
      .map(
        result =>
          if (result.failureDetails.isBlank)
            "*Test Executed and passed*"
          else
            result.failureDetails
      )
  )
end runSpec
