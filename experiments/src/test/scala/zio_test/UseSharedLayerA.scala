package zio_test

import zio.test.{TestAspect, ZIOSpec, assertCompletes}
import zio.*

object UseSharedLayerA extends ZIOSpec[Ref[Int]]:
  def bootstrap = Shared.layer

  def spec =
    test("Test A") {
      for _ <-
          ZIO.serviceWithZIO[Ref[Int]](
            _.update(_ + 1)
          )
      yield assertCompletes
    }
