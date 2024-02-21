package zio_test

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
      defer {
        val current =
          value.get.run
        s"**$current**"
      }

  val scoreBoard: ZLayer[
    Scope with Ref[Int],
    Nothing,
    Scoreboard
  ] =
    ZLayer.fromZIO {
      defer {
        val value =
          ZIO.service[Ref[Int]].run
        ZIO
          .acquireRelease(
            ZIO.succeed(Scoreboard(value)) <*
              ZIO.debug(
                "Initializing scoreboard!"
              )
          )(_ =>
            ZIO.debug("Shutting down scoreboard")
          )
          .run
      }
    }
end Shared
