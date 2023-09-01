# Concurrency

TODO Prose
```scala mdoc
def sleepThenPrint(
                    d: Duration
                  ): ZIO[Any, java.io.IOException, Duration] =
  defer {
    ZIO.sleep(d).run
    println(s"${d.render} elapsed")
    d
  }

```

```scala mdoc
runDemoValue(
    ZIO.foreach(Seq(2, 1)) { i =>
        sleepThenPrint(i.seconds)
    }
)

```

```scala mdoc
runDemoValue(
      ZIO.foreachPar(Seq(2, 1)) { i =>
        sleepThenPrint(i.seconds)
      }
)
```


```scala mdoc
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
```


```scala mdoc
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

```