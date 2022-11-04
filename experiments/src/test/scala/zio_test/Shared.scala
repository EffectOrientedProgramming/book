package zio_test

import zio.{Ref, Scope, ZIO, ZLayer}

object Shared:
  val layer: ZLayer[Any, Nothing, Ref[Int]] =
    ZLayer.scoped {
      ZIO.acquireRelease(
        Ref.make(0) <* ZIO.debug("Initializing!")
      )(
        _.get
          .debug(
            "Number of tests that used shared layer"
          )
      )
    }

  case class Scoreboard(value: Ref[Int]):
    def display(): ZIO[Any, Nothing, String] =
      for current <- value.get
      yield s"**$current**"

  val scoreBoard: ZLayer[
    Scope with Ref[Int],
    Nothing,
    Scoreboard
  ] =
    for
      value <- ZLayer.service[Ref[Int]]
      res <-
        ZLayer.scoped[Scope] {
          ZIO.acquireRelease(
            ZIO.succeed(Scoreboard(value.get)) <*
              ZIO.debug(
                "Initializing scoreboard!"
              )
          )(_ =>
            ZIO.debug("Shutting down scoreboard")
          )
        }
    yield res
end Shared
