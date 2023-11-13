## Parallelism

 

### experiments/src/main/scala/Parallelism/Finalizers.scala
```scala
package Parallelism

import zio.Console.printLine

import java.io.IOException
import scala.io.Source.*

object Finalizers extends zio.ZIOAppDefault:

  // In this example, we create a ZIO that uses
  // file IO. It opens a file to read it, but
  // gets failed half way through.
  // We use a finalizer to ensure that even if
  // the ZIO fails unexpectedly, the file will
  // still be closed.

  def finalizer(
      source: scala.io.Source
  ) = // Define the finalizer behavior here
    ZIO.succeed {
      println("Finalizing: Closing file reader")
      source.close // Close the input source
    }

  val readFileContents
      : ZIO[Scope, Throwable, Vector[String]] =
    ZIO
      .acquireRelease(
        ZIO.succeed(
          scala
            .io
            .Source
            .fromFile(
              "src/main/scala/Parallelism/csvFile.csv"
            )
        )
      )(finalizer)
      .map {
        bufferedSource => // Use the bracket method with the finalizer defined above to define behavior on fail.

          val lines = bufferedSource.getLines

          if (
            true
          ) // Simulating an enexpected error/exception
            throw new IOException("Boom!")

          Vector() ++ lines
      }

  def run = // Use App's run function
    println("In main")

    val ioExample: ZIO[
      Scope,
      Throwable,
      Unit
    ] = // Define the ZIO contexts
      defer {
        val fileLines = readFileContents.run
        printLine(fileLines.mkString("\n"))
          .run // Combine the strings of the output vector into a single string, separated by \n
      }

    ioExample
      .catchAllDefect(exception =>
        printLine(
          "Ultimate error message: " +
            exception.getMessage
        )
      )
      .exitCode // Call the Zio with exitCode.
  end run
end Finalizers

```


### experiments/src/main/scala/Parallelism/ParallelSleepers.scala
```scala
package Parallelism

val sleepNow =
  defer:
    ZIO
      .debug:
        "Yawn, going to sleep"
      .run
    ZIO
      .sleep:
        1.seconds
      .run
    ZIO
      .debug:
        "Okay, I am awake!"
      .run

import zio_helpers.timedSecondsDebug

@main
def quick =
  runDemo:
    defer:
      for _ <- 1 to 3 do
        sleepNow.run
    .timedSecondsDebug("Serial Sleepers")

object SerialSleepers extends ZIOAppDefault:
  override def run =
    defer:
      for _ <- 1 to 3 do
        sleepNow.run
    .timedSecondsDebug("Serial Sleepers")

object ParallelSleepers extends ZIOAppDefault:
  override def run =
    defer(Use.withParallelEval):
      for _ <- 1 to 3 do
        sleepNow.run
    .timedSecondsDebug("AllSleepers")

val sleepers =
  Seq(
    1.seconds,
    2.seconds,
    3.seconds,
    4.seconds,
    5.seconds
  )

object ParallelSleepers2 extends ZIOAppDefault:
  override def run =
    ZIO
      .foreach(sleepers)(ZIO.sleep(_))
      .timed
      .debug

object ParallelSleepers3 extends ZIOAppDefault:
  override def run =
    ZIO
      .foreachPar(sleepers)(ZIO.sleep(_))
      .timed
      .debug

object ParallelSleepers4 extends ZIOAppDefault:
  override def run =
    val racers = sleepers.map(ZIO.sleep(_))
    ZIO
      .raceAll(racers.head, racers.tail)
      .timed
      .debug

object ParallelSleepers5 extends ZIOAppDefault:
  override def run =
    ZIO.withParallelism(2) {
      ZIO
        .foreachPar(sleepers)(ZIO.sleep(_))
        .timed
        .debug
    }

```


### experiments/src/main/scala/Parallelism/PrimeSeeker.scala
```scala
package Parallelism

object PrimeSeeker extends ZIOAppDefault:

  override def run =
    ZIO
      .foreachPar(1 to 16): _ =>
        ZIO
          .succeed:
            crypto.nextPrimeAfter:
              100_000_000
          .debug:
            "Found prime:"
      .timed
      .debug:
        "Found a bunch of primes"

```


