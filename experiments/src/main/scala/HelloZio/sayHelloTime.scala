// sayHelloTime.scala
// This is a first look at using the Zio class
// with multiple error types

package HelloZio

import java.io
import zio.Console
import zio.Clock._

import java.util.concurrent.TimeUnit
import zio._
import java.io.IOException

object HelloWorldTime:

  // Now, becuase we are interacting with the
  // system clock, we need to add another error
  // type.
  case class NewError()

  @main
  def helloTime() =
    val sayHelloTime: ZIO[
      Has[Console] with Has[Clock], //implement the new environment type like a trait
      IOException | NewError, //implement the new error type using a | . (A logical 'or')
      Unit
    ] =
      for
        currentTime <-
          currentTime(TimeUnit.MILLISECONDS)
        _ <-
          Console.printLine(
            "Hello, World! Time: " + currentTime
          )
        _ <- ZIO.fail(NewError())
      yield ()
end HelloWorldTime
