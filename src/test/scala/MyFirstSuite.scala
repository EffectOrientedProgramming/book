import java.io

import zio.console.Console.Service
import zio.console
import zio.test
import zio.test.Assertion
import zio.test.environment
import zio._
import zio.clock._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import java.io.IOException

object HelloWorld:

  val sayHello: ZIO[
    zio.console.Console,
    IOException,
    Unit
  ] = console.putStrLn("Hello, World!")

// trait BigEnvironmentTrait extends
  // zio.console.Console with zio.clock.Clock

  case class NewError()

  val sayHelloWithAtime: ZIO[
    zio.console.Console with zio.clock.Clock,
    IOException | NewError,
    Unit
  ] =
    for
      currentTime <-
        currentTime(TimeUnit.MILLISECONDS)
      _ <-
        console.putStrLn(
          "Hello, World! Time: " + currentTime
        )
      _ <- ZIO.fail(NewError())
    yield ()

  // Equivalent to the above ^
  currentTime(TimeUnit.MILLISECONDS)
    .flatMap(currentTime =>
      console.putStrLn(
        "Hello, World! Time: " + currentTime
      )
    )
    .flatMap(_ => ZIO.fail(NewError()))
    .map(_ => ())

  def parameterizedHello(name: String): ZIO[
    zio.console.Console with zio.clock.Clock,
    IOException,
    String
  ] =
    for
      currentTime <-
        currentTime(TimeUnit.MILLISECONDS)
      _ <-
        console.putStrLn(
          s"Hello, $name! Time: " + currentTime
        )
    yield name
end HelloWorld

// val getName: ZIO[zio.console.Console,
// IOException, Unit] =
//    console.getS("Hello, World!")

object BadConsole:
  val singleFailure =
    new IOException("Bad stream")

  val live: Service =
    new Service:
      def putStr(line: String): UIO[Unit] = ???
      def putStrErr(line: String): UIO[Unit] =
        ???
      def putStrLnErr(line: String): UIO[Unit] =
        ???

      def putStrLn(
          line: String
      ): IO[IOException, Unit] =
        ZIO.fail(singleFailure)
      val getStrLn: IO[IOException, String] =
        ZIO.succeed("hardcoded string")
end BadConsole

object MyFirstSpec extends DefaultRunnableSpec:
  def spec =
    suite("HelloWorldSpec")(
      testM(
        "sayHello correctly displays output"
      ) {
        for
          result <-
            HelloWorld
              .sayHello
              .provideLayer(
                ZLayer.succeed(BadConsole.live)
              )
              .run
        yield assert(result)(
          fails(
            equalTo(BadConsole.singleFailure)
          )
        )
      }
    )
end MyFirstSpec
