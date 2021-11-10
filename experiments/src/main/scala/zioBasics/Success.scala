// Success.scala

package zioBasics

import zio._
import java.io
import java.io.IOException

object Success:

  @main
  def MainSuccess() =
    // success(1-4) are effects that succeed with
    // the specified value
    // You can pass in any parameter type,
    // including another ZIO
    val success1 =
      ZIO.succeed(
        12
      ) // This ZIO succeeds with a value of 12

    val success2 =
      ZIO.succeed(
        "Hello"
      ) // This ZIO succeeds with a value of Hello

    val bar = foo()
    val success3 =
      ZIO.succeed(
        bar
      ) // This ZIO succeeds with a class object

    val zioEx
        : ZIO[Has[Console], IOException, Unit] =
      Console.printLine("ZIO")
    val success4 =
      ZIO.succeed(
        zioEx
      ) // This ZIO succeeds with another ZIO

  println("Done")
end Success

case class foo()
