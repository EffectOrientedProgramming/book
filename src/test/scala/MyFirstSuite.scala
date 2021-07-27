import fakeEnvironmentInstances.FakeConsole

import java.io
import zio.Console
import zio.test
import zio.test.Assertion
import zio.test.environment
import zio.*
import zio.Clock.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.io.IOException

object HelloWorld:

  val sayHello
      : ZIO[Has[Console], IOException, Unit] =
    Console.printLine("Hello, World!")

// trait BigEnvironmentTrait extends
  // zio.console.Console with zio.clock.Clock

  case class NewError()

  val sayHelloWithAtime
      : ZIO[Has[Console] with Has[
        Clock
      ], IOException | NewError, Unit] =
    for
      currentTime <-
        currentTime(TimeUnit.MILLISECONDS)
      _ <-
        Console.printLine(
          "Hello, World! Time: " + currentTime
        )
      _ <- ZIO.fail(NewError())
    yield ()

  // Equivalent to the above ^
  currentTime(TimeUnit.MILLISECONDS)
    .flatMap(currentTime =>
      Console.printLine(
        "Hello, World! Time: " + currentTime
      )
    )
    .flatMap(_ => ZIO.fail(NewError()))
    .map(_ => ())

  def parameterizedHello(name: String): ZIO[Has[
    Console
  ] with Has[Clock], IOException, String] =
    for
      currentTime <-
        currentTime(TimeUnit.MILLISECONDS)
      _ <-
        Console.printLine(
          s"Hello, $name! Time: " + currentTime
        )
    yield name
end HelloWorld

// val getName: ZIO[zio.Console.Console,
// IOException, Unit] =
//    Console.getS("Hello, World!")

object MyFirstSpec extends DefaultRunnableSpec:
  def spec =
    suite("HelloWorldSpec")(
      test(
        "sayHello correctly displays output"
      ) {
        for
          result <-
            FakeConsole
              .singleInputConsole("blah")
              .readLine
              .exit
        yield assert(result)(
          succeeds(equalTo("blah"))
        )
      }
    )
end MyFirstSpec
