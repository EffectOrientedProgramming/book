# Concurrency Low Level

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
runDemo(
  defer {
    val f1 = sleepThenPrint(2.seconds).fork.run
    val f2 = sleepThenPrint(1.seconds).fork.run
    f1.join.run
    f2.join.run
  }
)
```
