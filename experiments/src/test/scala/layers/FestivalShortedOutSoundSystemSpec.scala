package layers

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object FestivalShortedOutSoundSystemSpec
    extends ZIOSpec[Festival]:
  val bootstrap =
    ZLayer.make[Festival](
      festival,
      fencing,
      stage,
      speakers,
      wires,
      amplifiers,
      soundSystemShortedOut,
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
end FestivalShortedOutSoundSystemSpec
