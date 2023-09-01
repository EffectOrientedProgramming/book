package zio_test_helpers

import zio.System
import zio.test.*
import zio.test.ReporterEventRenderer.ConsoleEventRenderer

object TestRunnerLocal {
  def runSpecAsApp(
                    spec: Spec[TestEnvironment with Scope, Any],
                    console: Console = Console.ConsoleLive,
                    aspects: Chunk[TestAspect[Nothing, Any, Nothing, Any]] = Chunk.empty,
                    testEventHandler: ZTestEventHandler = ZTestEventHandler.silent
                  )(implicit
                    trace: Trace
                  ): URIO[
    TestEnvironment with Scope,
    Summary
  ] = {

    for {
      runtime <-
        ZIO.runtime[
          TestEnvironment with Scope
        ]

      scopeEnv: ZEnvironment[Scope] = runtime.environment
      perTestLayer = (ZLayer.succeedEnvironment(scopeEnv) ++ liveEnvironment) >>>
        (TestEnvironment.live ++ ZLayer.environment[Scope])

      executionEventSinkLayer = ExecutionEventSink.live(Console.ConsoleLive, ConsoleEventRenderer)
      environment            <- ZIO.environment[Any]
      runner =
        TestRunner(
          TestExecutor
            .default[Any, Any](
              ZLayer.succeedEnvironment(environment),
              perTestLayer,
              executionEventSinkLayer,
              testEventHandler
            )
        )
      randomId <- ZIO.withRandom(Random.RandomLive)(Random.nextInt).map("test_case_" + _)
      summary <-
        runner.run(randomId, aspects.foldLeft(spec)(_ @@ _) @@ TestAspect.fibers)
    } yield summary
  }
}

object ProofOfConcept extends ZIOAppDefault:

  val liveEnvironment: Layer[Nothing, Clock with Console with System with Random] = {
    implicit val trace = Trace.empty
    ZLayer.succeedEnvironment(
      ZEnvironment[Clock, Console, System, Random](
        Clock.ClockLive,
        Console.ConsoleLive,
        System.SystemLive,
        Random.RandomLive
      )
    )
  }
  def runSpec[A](x: ZIO[Any, Nothing, TestResult]) =



    TestRunnerLocal.runSpecAsApp(
        zio.test.test("Default label")(x)
        //        .provide(
        //          ZLayer.environment[TestEnvironment with ZIOAppArgs with Scope] +!+
        //            (liveEnvironment >>> TestEnvironment.live +!+ TestLogger.fromConsole(Console.ConsoleLive))
        //        )
      )
      .provide(
        liveEnvironment,
        TestEnvironment.live,
        Scope.default
      )

  def run =
    val spec: ZIO[Any, Nothing, TestResult] =
      defer:
        val res = ZIO.succeed(43).run
        assertTrue(
          res == 43
        )
    runSpec(
      spec
    )

object HelloSpec extends ZIOSpecDefault:
  def spec =
    test("Hello")(
      defer:
        ZIO.debug("hi").run
        val res = ZIO.succeed(42).run
        assertTrue(
          res == 43
        )
    )

object HelloSpecIndirect extends ZIOSpecDefault:
  def spec =
    test("Hello")(
      for
        _ <- ZIO.debug("hi")
        res <- ZIO.succeed(42)
      yield assertTrue(
        res == 43
      )
    )
