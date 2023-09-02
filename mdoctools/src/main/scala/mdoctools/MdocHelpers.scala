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
        ZIO.succeed(Rendering.renderThrowable(error, columnWidth))
      case error: E =>
        ZIO.succeed(Rendering.renderError(error, topLineLength))
    }
    .catchAllDefect(defect =>
      val formattedMsg = Rendering.renderThrowableDefect(defect, topLineLength)
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

end wrapUnsafeZIOReportError


@annotation.nowarn
def runDemo[E, A](z: => ZIO[Any, E, A]): Unit =
  Unsafe.unsafe { (u: Unsafe) =>
    given Unsafe = u
    val res =
      unsafe
        .run(wrapUnsafeZIOReportError(z.withConsole(OurConsole)))
        .getOrThrowFiberFailure()
    // This is the *only* place we can trust to always print the final value
    println(res)
  }

// TODO Make a function that will execute a ZIO test case

import zio.System
import zio.test.*

def runSpec(x: ZIO[Any, Nothing, TestResult]) =

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

  runDemo(
    ZioTestExecution.runSpecAsApp(
        zio.test.test("")(x)
      )
      .provide(
        liveEnvironment,
        TestEnvironment.live,
        Scope.default
      )
      .map(_.failureDetails)
  )
