package mdoctools

import zio.System
import zio.test.*
import zio.test.ReporterEventRenderer.ConsoleEventRenderer


object ZioTestExecution {
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
      environment <- ZIO.environment[Any]
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
