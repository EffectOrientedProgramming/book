package layers

import zio.test.*

object FestivalSpec extends ZIOSpec[Festival]:
  val bootstrap =
    ZLayer.make[Festival](
      festival,
      stage,
      speakers,
      wires,
      amplifiers,
      soundSystem,
      toilets,
      foodtruck,
      security,
      venue,
      permit,
//      ZLayer.Debug.mermaid
    )

  val spec =
    suite("Play some music")(
      test("Good festival")(assertCompletes)
    )
end FestivalSpec
