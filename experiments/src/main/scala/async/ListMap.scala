package async

import zio.*
import zio.direct.*

object ListMap extends ZIOAppDefault:

  override def run =
    val numbers = Seq(1, 2, 3)

    /*
    numbers.map: i =>
      // some async thing
      i
    */

    val mapped = ZIO.foreach(numbers): i =>
      defer:
        ZIO.sleep(1.second).run
        i * 2

    mapped.debug