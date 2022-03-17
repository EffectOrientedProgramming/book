package handlingErrors

import zio.ZIO

object OrDie extends zio.ZIOAppDefault {
  val logic = for
    _ <- failable(-1).orDie
  yield ()

  def run = logic

  def failable(path: Int): ZIO[Any, Exception, String] =
    if (path < 0)
      ZIO.fail(new Exception("Negative path"))
//    else if (path > 0)
//      ZIO.fail("Too big")
    else
      ZIO.succeed("just right")
}
