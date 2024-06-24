```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val rosencrantzCoinToss =
  coinToss.debug:
    "R"

val rosencrantzAndGuildensternAreDead =
  defer:
    ZIO
      .debug:
        "*Performance Begins*"
      .run
    rosencrantzCoinToss.repeatN(4).run

    ZIO
      .debug:
        "G: There is an art to building suspense."
      .run
    rosencrantzCoinToss.run

    ZIO
      .debug:
        "G: Though it can be done by luck alone."
      .run
    rosencrantzCoinToss.run

    ZIO
      .debug:
        "G: ...probability"
      .run
    rosencrantzCoinToss.run
```

```scala 3 mdoc:testzio
import zio.test.*

def spec =
  test(
    "rosencrantzAndGuildensternAreDead finishes"
  ):
    defer:
      TestRandom
        .feedBooleans:
          true
        .repeatN:
          7
        .run
      rosencrantzAndGuildensternAreDead.run
      assertCompletes
```

```scala 3 mdoc:testzio
import zio.test.*

def spec =
  test("flaky plan"):
    defer:
      rosencrantzAndGuildensternAreDead.run
      assertCompletes
  @@ TestAspect.withLiveRandom @@
    TestAspect.flaky(Int.MaxValue)
```
