package layers

import zio.test.*

object FestivalSpec extends ZIOSpecDefault:

  val spec =
    suite("Play some music")(
      test("Good festival")(ZIO.service[Festival].as(assertCompletes)),
    ).provide(
        festival,
        stage,
        soundSystem,
        toilets,
        security,
        venue,
        permit,
        //      ZLayer.Debug.mermaid
    ) @@ TestAspect.withLiveClock
end FestivalSpec
