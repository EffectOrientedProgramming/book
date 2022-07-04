package bigdec

import zio._
import zio.test._

object UseSharedLayerA extends ZIOSpec[Int with Scope]{
  def spec = test("Test A") {
    for {
      sharedInt <- ZIO.service[Int]
    } yield assertTrue(sharedInt == 1)
  }

  def bootstrap =
    ZLayer.make[Environment](Scope.default, Shared.layer)
}
