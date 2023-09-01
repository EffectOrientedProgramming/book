# Concurrency - Interruption


```scala
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

```scala
runDemoValue(
  ZIO.raceAll(
    sleepThenPrint(2.seconds),
    Seq(sleepThenPrint(1.seconds))
  )
)
// 1 s elapsed
// res0: String = "PT1S"
```

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/concurrency_interruption.md)
