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
runDemo(
  ZIO.raceAll(
    sleepThenPrint(2.seconds),
    Seq(sleepThenPrint(1.seconds))
  )
)
// 1 s elapsed
// PT1S
```

We show that Future's are killed with finalizers that never run
```scala
import scala.concurrent.Future
runDemo(
  ZIO
    .fromFuture:
      Future:
        try Thread.sleep(500)
        finally println("Cleanup")
        "Success!"
    .timeout(25.millis)
)
// None
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/11_Concurrency_Interruption.md)
