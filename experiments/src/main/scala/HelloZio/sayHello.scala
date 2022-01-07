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

  // This is equivalent to the above, but the ZIO
  // definition is spaced across different lines.
  // As the ZIO definitions become more
  // complicated, it is more readable to space out
  // the definition.
  @main
  def hello2() =
    val sayHello2
        : ZIO[Console, IOException, Unit] =
      Console.printLine("Hello, World!")
end HelloWorld
