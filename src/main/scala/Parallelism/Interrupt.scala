package Parallelism

import java.io.IOException
import zio._
import zio.console._
import zio.duration.durationInt

class Interrupt {
  val n = 100

  //This ZIO does nothing but count to n.
  //It is not productive, but it uses resources.
  val countToN: ZIO[zio.clock.Clock, Nothing, Unit] =
    for _ <- ZIO.sleep(n.seconds)
    yield ()

  //This effect will create a fiber vrsion of countToN.
  //It will then interrupt the fiber, which returns an exit object.
  //Note: Interrupting Fibers is completely safe.
  //Interrupt safely releases all resources, and runs the finalizers.
  val noCounting: ZIO[zio.clock.Clock, Nothing, Exit[Nothing, Unit]] =
    for
      fiber <- countToN.fork
      exit <- fiber.interrupt
    yield exit

}
