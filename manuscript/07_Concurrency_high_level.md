# Concurrency

TODO Prose
```scala
def sleepThenPrint(
                    d: Duration
                  ): ZIO[Any, java.io.IOException, Duration] =
  defer {
    ZIO.sleep(d).run
    println(s"${d.render} elapsed")
    d
  }
```

```scala
runDemoValue(
    ZIO.foreach(Seq(2, 1)) { i =>
        sleepThenPrint(i.seconds)
    }
)
// 2 s elapsed
// 1 s elapsed
// res0: String = "List(PT2S, PT1S)"
```

```scala
runDemoValue(
      ZIO.foreachPar(Seq(2, 1)) { i =>
        sleepThenPrint(i.seconds)
      }
)
// 1 s elapsed
// 2 s elapsed
// res1: String = "List(PT2S, PT1S)"
```


```scala
runDemoValue(
  defer {
    val durations =
      ZIO
        .collectAllPar(
          Seq(
            sleepThenPrint(2.seconds),
            sleepThenPrint(1.seconds)
          )
        )
        .run
    val total =
      durations
        .fold(Duration.Zero)(_ + _)
        .render
    Console.printLine(total).run
  }
)
// 1 s elapsed
// 2 s elapsed
// res2: String = "()"
```


```scala
// Massive example
runDemoValue(
  defer {
    val durations =
      ZIO
        .collectAllSuccessesPar(
          Seq
            .fill(1_000)(1.seconds)
            .map(duration =>
              defer {
                val randInt =
                  Random
                    .nextIntBetween(0, 100)
                    .run
                ZIO.sleep(duration).run
                ZIO
                  .when(randInt < 10)(
                    ZIO.fail(
                      "Number is too low"
                    )
                  )
                  .run
                duration
              }
            )
        )
        .run
    val total =
      durations
        .fold(Duration.Zero)(_ + _)
        .render
    Console.printLine(total).run
  }
)
// res3: String = "()"
```

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/07_Concurrency_high_level.md)
