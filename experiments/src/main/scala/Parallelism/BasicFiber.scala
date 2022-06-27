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
