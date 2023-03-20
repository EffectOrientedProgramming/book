## experiments-src-test-scala-layers
 Newer stuff!
 

### experiments/src/test/scala/layers/FestivalFencingUnavailableSpec.scala
```scala
package layers

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object FestivalFencingUnavailableSpec
    extends ZIOSpecDefault:
  val missingFencing: ZIO[Any, String, Fencing] =
    ZIO.fail("No fencing!")
  private val brokenFestival
      : ZLayer[Any, String, Festival] =
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
end FestivalFencingUnavailableSpec

```


### experiments/src/test/scala/layers/FestivalShortedOutSoundSystemSpec.scala
```scala
package layers

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object FestivalShortedOutSoundSystemSpec
    extends ZIOSpecDefault:
  val brokenFestival
      : ZLayer[Any, String, Festival] =
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

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object FestivalSpec extends ZIOSpec[Festival]:
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
      permit
    )

  val spec =
    suite("Play some music")(
      test("Good festival")(
        assertCompletes
      )
    )
end FestivalSpec

```


