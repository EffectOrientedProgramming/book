package zio_test

import zio.*
import zio.test.*
import zio_test.Shared.Scoreboard

object UseComplexLayer
    extends ZIOSpec[Scoreboard]:
  def bootstrap
      : ZLayer[Any, Nothing, Scoreboard] =
    ZLayer.make[Scoreboard](
      Shared.layer,
      Shared.scoreBoard,
      Scope.default
    )

  def spec =
    test("use scoreboard") {
      for _ <-
          ZIO
            .serviceWithZIO[Scoreboard](
              _.display()
            )
            .debug
      yield assertCompletes
    }
end UseComplexLayer
