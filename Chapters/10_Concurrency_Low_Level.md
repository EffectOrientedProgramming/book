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
runDemo(
  ZIO.foreach(Seq(2, 1)) { i =>
    sleepThenPrint(i.seconds)
  }
)
```

```scala mdoc
runDemo(
  ZIO.foreachPar(Seq(2, 1)) { i =>
    sleepThenPrint(i.seconds)
  }
)
```


```scala mdoc
runDemo(
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
      durations.fold(Duration.Zero)(_ + _).render
    Console.printLine(total).run
  }
)
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
