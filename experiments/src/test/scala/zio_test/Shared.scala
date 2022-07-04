package zio_test

import zio.{Ref, Scope, ZIO, ZLayer}

object Shared:
  val layer: ZLayer[Scope, Nothing, Ref[Int]] =
    ZLayer.scoped[Scope] {
      ZIO.acquireRelease(
        Ref.make(0) <* ZIO.debug("Initializing!")
      )(
        _.get
          .debug(
            "Number of tests that used shared layer"
          )
      )
    }
