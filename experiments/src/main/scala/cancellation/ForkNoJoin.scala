package cancellation

import zio.*

object ForkNoJoin extends ZIOAppDefault:

  override def run =
    ZIO.sleep(Duration.Infinity)
      .onInterrupt(Console.printLine("interrupted").orDie)
      .fork