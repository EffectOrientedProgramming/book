package Parallelism

import java.io.IOException
import zio.console.{
  Console,
  getStrLn,
  putStrLn
}
import zio.{
  IO,
  Runtime,
  ZIO,
  ZLayer,
  UIO,
  Fiber
}

class Join {

  //Joining a fiber converts it into an effect.
  //This effect will succeed or fail depending on the fiber.
  val joinedFib100
      : UIO[Long] = //This function makes a fiber, then joins the fiber, and returns it as an effect
    for
      fiber <- computation
        .fib(100)
        .fork //Fiber is made to find 100th value of Fib
      output <-
        fiber.join //Fiber is converted into an effect, then returned.
    yield output

  //This object performs a computation that takes a long time. It is a recursive Fibonacci Sequence generator.
  object computation {

    def fib(n: Long): UIO[Long] = UIO {
      if (n <= 1) UIO.succeed(n)
      else
        fib(n - 1).zipWith(fib(n - 2))(_ + _)
    }.flatten
  }

}
