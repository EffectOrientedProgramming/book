package cause

import zio._

object CauseBasics extends ZIOAppDefault {
  def run =
//    ZIO.fail(Cause.fail("Blah"))
    ZIO.fail(Cause.die(Exception("Blah")) ++ Cause.die(Exception("Dee")))

}
