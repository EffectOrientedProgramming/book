package Parallelism

import java.io.IOException
import zio._
import zio.console._
import zio.Fiber._

class Compose {
  //Composing Fibers will combine 2 or more fibers into a single fiber. This new fiber
  //will produce the results of both. If any of the fibers fail, the entire zipped fiber
  //will also fail.

  //Note: The results of the zipped fibers will be put into a tuple.

  val helloGoodbye: UIO[Tuple] =
    for
      greeting <- IO.succeed("Hello!").fork
      fairwell <- IO.succeed("GoodBye!").fork
      totalFiber = greeting.zip(
        fairwell
      ) //Note the '=', not '<-'
      tuple <- totalFiber.join
    yield tuple

  //A very useful fiber method or composing is the 'orElse' method.
  //This method will combine two fibers. If the first succeeds, the composed fiber will
  //succeed with first fiber's result. If the first fails, the second will be used.

  val isPineapple: IO[String, String] =
    IO.succeed("Pineapple!")

  val notPineapple: IO[String, String] =
    IO.fail("Banana...")

  val composeFruit: IO[String, String] =
    for
      fFiber <-
        notPineapple.fork //notPineapple will fail
      sFiber <-
        isPineapple.fork //isPineapple will succedd
      totalFiber = fFiber.orElse(sFiber)
      output <-
        totalFiber.join //The output effect will end up using isPineapple.
    yield output

}
