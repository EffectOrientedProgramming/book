package mdoctools

import zio.Runtime.default.unsafe

import java.io.IOException

object Stuff:
  object WithALongName:
    object ThatWillComplicate:
      def run =
        throw new Exception(
          "Boom stoinky kablooey pow pow pow"
        )

val commentPrefix = "// "
val columnWidth =
  49 -
    commentPrefix
      .length // TODO Pull from scalafmt config file

private def renderThrowable(
    error: Throwable
): String =
  error
    .toString
    .split("\n")
    .map(line =>
      if (line.length > columnWidth)
        throw new Exception(
          "Need to handle stacktrace line: " +
            line
        )
      else
        line
    )
    .mkString("\n")

// Consider crashing if output is unexpectedly long
def wrapUnsafeZIOReportError[E, A](
    z: => ZIO[Any, E, A]
): ZIO[Any, java.io.IOException, String] =
  val defectPrefix = "Error: "
  val topLineLength =
    columnWidth - defectPrefix.length
  z.map(result => result.toString)
    .catchAll {
      case error: Throwable =>
        ZIO.succeed(renderThrowable(error))
      case error: E =>
        val extractedMessage = error.toString
        val formattedMsg =
          if (
            extractedMessage.length >
              topLineLength
          )
            extractedMessage.take(topLineLength)
          else
            extractedMessage

        ZIO.succeed(formattedMsg)
    }
    .catchAllDefect(defect =>
      val msg = defect.toString
      val extractedMessage =
        if (msg != null && msg.nonEmpty)
          if (msg.contains("$"))
            msg
              .split("\\$")
              .last
              .replace(")", "")
          else
            msg
        else
          ""
      val formattedMsg =
        if (
          extractedMessage.length > topLineLength
        )
          extractedMessage.take(topLineLength)
        else
          extractedMessage

      ZIO.succeed("Defect: " + formattedMsg)
    )
    .map { result =>
      result
        .split("\n")
        .map(line =>
          if (line.length > columnWidth)
            println(
              "Need to handle long line. \n" +
                "Truncating for now: \n" + line
            )
            line.take(columnWidth)
          else
            line
        )
        .mkString("\n")
      // TODO Respect width limit
    }
//    .tap(finalValueToRender =>
//      Console.printLine(finalValueToRender)
//    )

end wrapUnsafeZIOReportError

object OurConsole extends Console:
  override def print(line: => Any)(implicit
      trace: Trace
  ): IO[IOException, Unit] = ???

  override def printError(line: => Any)(implicit
      trace: Trace
  ): IO[IOException, Unit] = ???

  override def printLine(line: => Any)(implicit
      trace: Trace
  ): IO[IOException, Unit] =
    ZIO.succeed(println(line))

  override def printLineError(line: => Any)(
      implicit trace: Trace
  ): IO[IOException, Unit] = ???

  override def readLine(implicit
      trace: Trace
  ): IO[IOException, String] = ???
end OurConsole

@annotation.nowarn
def runDemo[E, A](z: => ZIO[Any, E, A]): Unit =
  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    val res =
      unsafe
        .run(
          wrapUnsafeZIOReportError(
            z.withConsole(OurConsole)
          )
        )
        .getOrThrowFiberFailure()
    println(res)
  }

// TODO Make a function that will execute a ZIO test case

import zio.System
import zio.test.*
import zio.test.ReporterEventRenderer.ConsoleEventRenderer

object TestRunnerLocal:
  def runSpecAsApp(
      spec: Spec[
        TestEnvironment with Scope,
        Any
      ],
      console: Console = Console.ConsoleLive,
      aspects: Chunk[
        TestAspect[Nothing, Any, Nothing, Any]
      ] = Chunk.empty,
      testEventHandler: ZTestEventHandler =
        ZTestEventHandler.silent
  )(implicit
      trace: Trace
  ): URIO[TestEnvironment with Scope, Summary] =
    for
      runtime <-
        ZIO.runtime[TestEnvironment with Scope]

      scopeEnv: ZEnvironment[Scope] =
        runtime.environment
      perTestLayer =
        (ZLayer.succeedEnvironment(scopeEnv) ++
          liveEnvironment) >>>
          (TestEnvironment.live ++
            ZLayer.environment[Scope])

      executionEventSinkLayer =
        ExecutionEventSink.live(
          Console.ConsoleLive,
          ConsoleEventRenderer
        )
      environment <- ZIO.environment[Any]
      runner =
        TestRunner(
          TestExecutor.default[Any, Any](
            ZLayer
              .succeedEnvironment(environment),
            perTestLayer,
            executionEventSinkLayer,
            testEventHandler
          )
        )
      randomId <-
        ZIO
          .withRandom(Random.RandomLive)(
            Random.nextInt
          )
          .map("test_case_" + _)
      summary <-
        runner.run(
          randomId,
          aspects.foldLeft(spec)(_ @@ _) @@
            TestAspect.fibers
        )
    yield summary
end TestRunnerLocal

def runSpec(x: ZIO[Any, Nothing, TestResult]) =

  val liveEnvironment: Layer[
    Nothing,
    Clock with Console with System with Random
  ] =
    implicit val trace = Trace.empty
    ZLayer.succeedEnvironment(
      ZEnvironment[
        Clock,
        Console,
        System,
        Random
      ](
        Clock.ClockLive,
        Console.ConsoleLive,
        System.SystemLive,
        Random.RandomLive
      )
    )

  runDemo(
    TestRunnerLocal
      .runSpecAsApp(
        zio
          .test
          .test("")(
            x.tap(details =>
              println("Details: " + details)
              ZIO.succeed(
                println("Details: " + details)
              )
            )
          )
          //        .provide(
          // ZLayer.environment[TestEnvironment
          // with ZIOAppArgs with Scope] +!+
          // (liveEnvironment >>>
          // TestEnvironment.live +!+
          // TestLogger.fromConsole(Console.ConsoleLive))
          //        )
      )
      .provide(
        liveEnvironment,
        TestEnvironment.live,
        Scope.default
      )
      .map { x =>
        scala.Console.println("Failure!")
        println(x.failureDetails)
        x.failureDetails
      }
      .tap(details =>
        println("Details: " + details)
        ZIO.succeed(
          println("Details: " + details)
        )
      )
  )
end runSpec
