package fibers

import zio.*
import zio.Console.*

object HowMany extends ZIOAppDefault:
  def run =
    ZIO.foreachPar(Range(0, 1_000_000))(_ =>
      ZIO.sleep(1.second)
    )
