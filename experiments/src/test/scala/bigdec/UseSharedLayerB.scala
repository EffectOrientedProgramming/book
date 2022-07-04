package bigdec

import zio.*
import zio.test.*

object UseSharedLayerB extends ZIOSpec[Int]{
  def spec = test("Test B") {
    for {
      sharedInt <- ZIO.service[Int]
    } yield assertTrue(sharedInt == 1)
  }

  def bootstrap = Shared.layer

}
