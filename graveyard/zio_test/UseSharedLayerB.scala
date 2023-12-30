package zio_test

import zio.test.{ZIOSpec, assertCompletes}
import zio.{Ref, ZIO}

object UseSharedLayerB extends ZIOSpec[Ref[Int]]:
  def bootstrap = Shared.layer

  def spec =
    test("Test B") {
      for _ <-
          ZIO.serviceWithZIO[Ref[Int]](count =>
            count.update(_ + 1)
          )
      yield assertCompletes
    }
