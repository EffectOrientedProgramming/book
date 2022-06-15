package mutability

import zio.{Ref, ZIO, ZIOAppDefault}

object Refs extends ZIOAppDefault {
  def run =
    for
      ref <- Ref.make(1)
      startValue <- ref.get
      _ <- ZIO.debug("start value: " + startValue)
      _ <- ref.set(5)
      finalValue <- ref.get
      _ <- ZIO.debug("final value: " + finalValue)
    yield ()

}
