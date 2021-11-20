// sayHello.scala
// This is a first look at using the Zio class

package HelloZio

import java.io
import zio._
import java.io.IOException

object HelloWorld:

  @main
  def hello() =
    val sayHello
        : ZIO[Console, IOException, Unit] =
      Console.printLine("Hello, World!")

  // This is equivelant to the above, but the Zio
  // definition is spaced across different lines.
  // As the Zio definitions become more
  // comlicated, it is more readable to space out
  // the definition.
  @main
  def hello2() =
    val sayHello2
        : ZIO[Console, IOException, Unit] =
      Console.printLine("Hello, World!")
end HelloWorld
