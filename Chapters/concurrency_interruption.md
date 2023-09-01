# Concurrency - Interruption


```scala mdoc
// This is duplicate code
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
  ZIO.raceAll(
    sleepThenPrint(2.seconds),
    Seq(sleepThenPrint(1.seconds))
  )
)
```