## Parallelism

 

### experiments/src/main/scala/Parallelism/BasicFiber.scala
```scala
package Parallelism

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
    computation.fib(100).fork

  // Part of the power of Fibers is that many of
  // them can be described and run at once.
  // This function uses two numbers (n and m),
  // and outputs two Fibers that will find the
  // n'th and m'th Fibonacci numbers
  val n: Long = 50
  val m: Long = 100

  val fibNandM
      : UIO[Vector[Fiber[Nothing, Long]]] =
    defer {
      val fiberN = computation.fib(n).fork.run
      val fiberM = computation.fib(m).fork.run
      Vector(fiberN, fiberM)
    }
end BasicFiber

```


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

object ParallelSleepers extends ZIOAppDefault:

  override def run =
    defer(Use.withParallelEval) {
      for _ <- 1 to 10_000 do
        ZIO.sleep(1.seconds).run

      ZIO
        .debug(
          "Finished far sooner than 10,000 seconds"
        )
        .run
    }

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
    ZIO.withParallelism(1) {
      ZIO.foreachPar(1 to 16) { _ =>
        ZIO.succeed(
          crypto.nextPrimeAfter(100_000_000)
        )
      }
    } *> ZIO.debug("Found a bunch of primes")

```


### experiments/src/main/scala/Parallelism/UseAllTheThreads.scala
```scala
package Parallelism

import java.lang.Runtime as JavaRuntime

/* This example was a port of Bruce's Python
 * example:
 * https://github.com/BruceEckel/python-experiments/blob/main/parallelism/cpu_intensive.py
 *
 * We did this port to explore some very basic
 * parallelism.
 * We learned how in ZIO we generally shouldn't
 * base concurrency on the number of system cores
 * because ZIO takes care of that for us. */
object UseAllTheThreads extends ZIOAppDefault:

  def cpuIntensive(
      n: Int,
      multiplier: Int
  ): Double =
    /* // mutable is fast var result: Double = 0
     * for (i <- 0 until 10_000_000 * multiplier)
     * { result += Math.sqrt(Math.pow(i, 3) +
     * Math.pow(i, 2) + i * n) } result */
    /* // this is very slow due to massive
     * allocations (0 until 10_000_000 *
     * multiplier).map { i =>
     * Math.sqrt(Math.pow(i, 3) + Math.pow(i, 2)
     * + i * n) }.sum */
    // pretty fast and immutable
    (0 until 10_000_000 * multiplier)
      .foldLeft(0d) { (result, i) =>
        result +
          Math.sqrt(
            Math.pow(i, 3) + Math.pow(i, 2d) +
              i * n
          )
      }

  override def run =
    defer {
      // note that we used numCores to show how
      // we can saturate all of them
      // but it turns out that even with a number
      // less than the number of cores
      // we can still saturate all of them
      // because the ZIO scheduler time slices
      val numCores =
        JavaRuntime
          .getRuntime
          .availableProcessors
      ZIO
        .foreachPar(0 to numCores) { i =>
          ZIO.succeed(cpuIntensive(i, 50))
        }
        .debug
        .run
    }
end UseAllTheThreads

```


