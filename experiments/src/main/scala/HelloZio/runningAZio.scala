// runningAZio.scala

package HelloZio

import java.io
import zio._
import java.io.IOException

// How to run a basic Zio
object HelloApp
    extends zio.App: // Extend the Zio App

  val myAppLogic: ZIO[ // Define the Zio
    Console,
    IOException,
    Unit
  ] = Console.printLine("Hello, World!")

  def run(
      args: List[String]
  ) = // Use App's run function
    myAppLogic
      .exitCode // Call the Zio with exitCode.

// Zio's run function which we inherit from
// zio.App needs a ZIO exitCode as a return type.
// If you use the run function to excecute your
// ZIO program, then you need to structure your
// code to end on an exitCode.
