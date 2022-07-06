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
//      ZLayer.Debug.mermaid
    )


  val spec =
    test("festival time")(
      assertCompletes
    )
}
