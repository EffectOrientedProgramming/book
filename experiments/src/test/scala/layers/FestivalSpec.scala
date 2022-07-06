package layers

import zio.*
import zio.test.*

object FestivalSpec extends ZIOSpec[Festival] {
  val bootstrap =
    ZLayer.make[Festival](
      festival,
      fencing,
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
    )


  val spec =
    suite("Play some music")(
      test("Song 1")(
        assertCompletes
      ),
      test("Song 2")(
        assertCompletes
      ),
    )
}
