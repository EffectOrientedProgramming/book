package layers

import zio.test.*

object FestivalShortedOutSoundSystemSpec
    extends ZIOSpecDefault:
  val brokenFestival
      : ZLayer[Any, String, Festival] =
    ZLayer.make[Festival](
      festival,
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
      test("Good festival")(
        (
          for _ <- ZIO.service[Festival]
          yield assertCompletes
        ).provide(brokenFestival)
          .withClock(Clock.ClockLive)
          .catchAll(e =>
            ZIO.debug("Expected error: " + e) *>
              ZIO.succeed(assertCompletes)
          )
      )
    )
end FestivalShortedOutSoundSystemSpec
