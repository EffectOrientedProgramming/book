package layers

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object FestivalFencingUnavailableSpec
    extends ZIOSpecDefault:
  val missingFencing: ZIO[Any, String, Fencing] =
    ZIO.fail("No fencing!")
  private val brokenFestival: ZLayer[Any, String, Festival] =
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
      test("Good festival")(
        (for
          _ <- ZIO.service[Festival]
        yield assertCompletes)
          .provide(brokenFestival)
          .withClock(Clock.ClockLive)
          .catchAll(e => ZIO.debug("Expected error: " + e) *> ZIO.succeed(assertCompletes))
      )
    )
end FestivalFencingUnavailableSpec
