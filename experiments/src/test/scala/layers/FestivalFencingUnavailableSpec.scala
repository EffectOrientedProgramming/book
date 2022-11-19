package layers

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object FestivalFencingUnavailableSpec
    extends ZIOSpec[Festival]:
  val missingFencing: ZIO[Any, String, Fencing] =
    ZIO.fail("No fencing!")
  val bootstrap =
    ZLayer.make[Festival](
      festival,
      ZLayer.fromZIO(missingFencing),
      stage,
      speakers,
      wires,
      amplifiers,
      soundSystem,
      toilets,
      foodtruck,
      security,
      venue,
      permit
    )

  val spec =
    suite("Play some music")(
      test("Good festival")(assertCompletes)
    )
end FestivalFencingUnavailableSpec
