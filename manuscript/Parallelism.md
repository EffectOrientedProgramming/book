## Parallelism

 

### experiments/src/main/scala/Parallelism/BasicFiber.scala
```scala
package Parallelism

import java.io.IOException
import zio.Console
import zio.{Fiber, IO, Runtime, UIO, ZIO, ZLayer}

object BasicFiber:

  // Fibers model a running IO: Fiber[E,A]. They
  // have an error type, and a success type.
  // They don't need an input environment type.
  // They are not technically effects, but they
  // can be converted to effects.

  object computation: // This object performs a computation that takes a long time. It is a recursive Fibonacci Sequence generator.

    def fib(n: Long): UIO[Long] =
      ZIO
        .succeed {
          if (n <= 1)
            ZIO.succeed(n)
          else
            fib(n - 1).zipWith(fib(n - 2))(_ + _)
        }
        .flatten

  // Fork will take an effect, and split off a
  // Fiber version of it.
  // This ZIO will output a Fiber that is
  // computing the 100th digit of the Fibonacci
  // Sequence.
  val fib100: UIO[Fiber[Nothing, Long]] =
    for fiber <- computation.fib(100).fork
    yield fiber

  // Part of the power of Fibers is that many of
  // them can be described and run at once.
  // This function uses two numbers (n and m),
  // and outputs two Fibers that will find the
  // n'th and m'th Fibonacci numbers
  val n: Long = 50
  val m: Long = 100

  val fibNandM
      : UIO[Vector[Fiber[Nothing, Long]]] =
    for
      fiberN <- computation.fib(n).fork
      fiberM <- computation.fib(m).fork
    yield Vector(fiberN, fiberM)
end BasicFiber

```


### experiments/src/main/scala/Parallelism/Finalizers.scala
```scala
package Parallelism

import java.io.IOException
import zio.Console.printLine
import zio.{
  Console,
  Fiber,
  IO,
  Runtime,
  Scope,
  UIO,
  URIO,
  ZIO,
  ZLayer
}

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

          val lines =
            for line <- bufferedSource.getLines
            yield line

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
      for
        fileLines <- readFileContents
        _ <-
          printLine(
            fileLines.mkString("\n")
          ) // Combine the strings of the output vector into a single string, separated by \n
      yield ()
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

import java.io.IOException
import zio.{
  Fiber,
  IO,
  Runtime,
  UIO,
  Unsafe,
  ZIO,
  ZIOAppDefault,
  ZLayer,
  durationInt
}

import scala.concurrent.Await

object ParallelSleepers extends ZIOAppDefault:

  override def run =
    ZIO.foreachPar(1 to 10_000)(_ =>
      ZIO.sleep(1.seconds)
    ) *>
      ZIO.debug(
        "Finished far sooner than 10,000 seconds"
      )

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

import zio.*

import java.math.BigInteger

object PrimeSeeker extends ZIOAppDefault:

  override def run =
    ZIO.withParallelism(1) {
      ZIO.foreachPar(1 to 16) { _ =>
        ZIO.succeed(
          crypto.nextPrimeAfter(100_000_000)
        )
      }
    } *> ZIO.debug("Found a bunch of primes")

```


