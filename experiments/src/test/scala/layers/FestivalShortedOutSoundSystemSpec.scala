package layers

import zio.test.*

// TODO Replace with some toilet issue?
object FestivalShortedOutSoundSystemSpec
    extends ZIOSpecDefault:
  val brokenFestival
      : ZLayer[Any, String, Festival] =
    ZLayer.make[Festival](
      festival,
      stage,
      soundSystem,
      toilets,
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
