package zio_test

import zio.test.{
  TestAspect,
  ZIOSpec,
  assertCompletes
}
import zio.{Ref, Scope, ZIO, ZLayer}


object UseSharedLayerB extends ZIOSpec[Ref[Int]]:
  def bootstrap = Shared.layer

  def spec =
    test("Test B") {
      for {
        _ <- ZIO.serviceWithZIO[Ref[Int]] ( count => count.update(_ + 1) )
      } yield assertCompletes
    }
