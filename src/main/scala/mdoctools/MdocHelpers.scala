package mdoctools

import zio.test.ReporterEventRenderer.ConsoleEventRenderer
import zio.test.*

import java.util.concurrent.{
  LinkedBlockingQueue,
  ThreadPoolExecutor,
  TimeUnit
}

extension [R, E, A](z: ZLayer[R, E, A])
  def tapWithMessage(
      message: String
  ): ZLayer[R, E, A] =
    z.tap(
      _ => ZIO.succeed(println(message))
    )
//    z.zipParLeft(background.forkDaemon)

trait ToRun(
    useLiveClock: Boolean =
      false
):
  val bootstrap: ZLayer[Any, Nothing, Any] =
    ZLayer.empty

  def run: ZIO[Scope, Any | Nothing, Any]

  def runAndPrintOutput(): Unit =
    // override the PrintStream in OurConsole
    // with the one that mdoc sets
    val ourConsole =
      OurConsole(Some(scala.Console.out))
    val ourClock =
      OurClock(useLiveClock)

    Unsafe.unsafe:
      implicit unsafe =>
        // using the ThreadPoolExecutor prevents
        // the overwriting of scala.Console.out
        val myBootstrap =
          Runtime.setExecutor(
            Executor.fromThreadPoolExecutor(
              new ThreadPoolExecutor(
                5,
                10,
                5000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue[
                  Runnable
                ]()
              )
            )
          )

        val e =
          Rendering
            .renderEveryPossibleOutcomeZio:
              ZIO.scoped: // run this in a scope to insure that the finalizers run in a scope for the ToRun
                run
            .withConsole(ourConsole)
            .withClock(ourClock)

        val ourEnvironment =
          ZEnvironment[
            Clock,
            Console,
            System,
            Random
          ](
            ourClock,
            ourConsole,
            System.SystemLive,
            Random.RandomLive
          )

        val result =
          Runtime
            .unsafe
            .fromLayer(myBootstrap ++ bootstrap)
            .withEnvironment(ourEnvironment) // just to be extra sure that the Console & Clock are ours
            .unsafe
            .run(e) match
            case Exit.Success(value) =>
              value
            case Exit.Failure(cause) =>
              throw new IllegalStateException(
                "Failed to run"
              )

        if result != "()" then
          println(s"Result: $result")
  end runAndPrintOutput
end ToRun

abstract class ToTest extends ToRun:
  def spec: Spec[TestEnvironment & Scope, Any]

  // todo: E A instead of Any
  override def run
      : ZIO[Scope, Any | Nothing, Any] =
    // override the PrintStream in OurConsole
    // with the one that mdoc sets
    val ourConsole =
      OurConsole(Some(scala.Console.out))

    val ourEnvironment =
      ZEnvironment[
        Clock,
        Console,
        System,
        Random
      ](
        OurClock(),
        ourConsole,
        System.SystemLive,
        Random.RandomLive
      )

    val ourEnvironmentLayer: Layer[
      Nothing,
      Clock & Console & System & Random
    ] =
      implicit val trace =
        Trace.empty
      ZLayer.succeedEnvironment(ourEnvironment)

    val executionEventSinkLayer =
      ExecutionEventSink
        .live(ourConsole, ConsoleEventRenderer)

    val ourTestEnvironmentLayer: ZLayer[
      Clock & Console & System & Random,
      Nothing,
      TestEnvironment
    ] =
      Annotations.live ++ Live.default ++
        Sized.live(100) ++
        TestConfig.live(100, 100, 200, 1000) ++
        ((Live.default ++ Annotations.live) >>>
          TestConsole.debug) ++
        TestRandom.deterministic ++
        TestSystem.default

    val specLayers =
      ourEnvironmentLayer >>>
        ourTestEnvironmentLayer

    val testExecutor: TestExecutor[
      TestEnvironment & Scope,
      Any
    ] =
      TestExecutor.default[TestEnvironment, Any](
        specLayers,
        Scope.default ++ specLayers,
        executionEventSinkLayer,
        ZTestEventHandler.silent
      )

    val runner =
      TestRunner(testExecutor)

    defer:
      val wrappedSpec =
        spec
          .execute(ExecutionStrategy.Sequential)
          .provide(Scope.default ++ specLayers)
          .withConsole(ourConsole)
          .run
      val summary =
        runner
          .run(
            "none",
            wrappedSpec,
            ExecutionStrategy.Sequential
          )
          .withConsole(ourConsole)
          .run
      summary.status match
        case Summary.Success =>
          ZIO.unit.run
        case Summary.Failure =>
          ZIO.unit.run
  end run
end ToTest
