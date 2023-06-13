package zio_test

import zio.*
import zio.direct.*
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
      defer {
        ZIO
          .serviceWithZIO[Scoreboard](
            _.display()
          )
          .debug
          .run
        assertCompletes
      }
    }
end UseComplexLayer
