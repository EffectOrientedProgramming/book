//sayHelloTime.scala
//This is a first look at using the Zio class with multiple error types

package HelloZio

import java.io
import zio.console.Console.Service
import zio.console
import zio.clock._

import java.util.concurrent.TimeUnit
import zio._
import java.io.IOException

object HelloWorldTime:

  //Now, becuase we are interacting with the system clock, we need to add another error type.
  case class NewError()

  @main def helloTime() =
    val sayHelloTime: ZIO[
      zio.console.Console with zio.clock.Clock, //implement the new environment type like a trait
      IOException |
        NewError, //implement the new error type using a | . (A logical 'or')
      Unit
    ] =
      for {
        currentTime <- currentTime(TimeUnit.MILLISECONDS)
        _ <- console.putStrLn("Hello, World! Time: " + currentTime)
        _ <- ZIO.fail(NewError())
      } yield ()
