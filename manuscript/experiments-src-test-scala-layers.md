## experiments-src-test-scala-layers

 

### experiments/src/test/scala/layers/FestivalShortedOutSoundSystemSpec.scala
```scala
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

```


### experiments/src/test/scala/layers/FestivalSpec.scala
```scala
package layers

import zio.test.*

object FestivalSpec extends ZIOSpec[Festival]:
  val bootstrap =
    ZLayer.make[Festival](
      festival,
      stage,
      soundSystem,
      toilets,
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

```


