package cause

import zio._

object CauseBasics extends App {
//    ZIO.fail(Cause.fail("Blah"))
    println(
      (Cause.die(Exception("1")) ++
        (Cause.fail(Exception("2a")) && Cause.fail(Exception("2b"))) ++
          Cause.stackless(Cause.fail(Exception("3"))
          )
        ).prettyPrint)

}

object CauseZIO extends ZIOAppDefault:

  val x: ZIO[Any, Nothing, Nothing] =
    ZIO.die(Exception("Blah"))
  def run =
    ZIO.die(Exception("Blah"))