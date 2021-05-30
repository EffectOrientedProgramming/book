//Success.scala

package effects

import zio._
import java.io
import java.io.IOException

object Success {

  @main def MainSuccess() =
    //success(1-4) are effects that succeed with the specified value
    //You can pass in any parameter type, including another ZIO
    val success1 = ZIO.succeed(12)

    val success2 = ZIO.succeed("Hello")

    val bar = foo()
    val success3 = ZIO.succeed(bar)

    val zioEx: ZIO[zio.console.Console, IOException, Unit] =
      console.putStrLn("ZIO")
    val success4 = ZIO.succeed(zioEx)

  println("Done")
}

case class foo()
