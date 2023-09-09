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
runDemo(
  ZIO.foreach(Seq(2, 1)) { i =>
    sleepThenPrint(i.seconds)
  }
)
// 2 s elapsed
// 1 s elapsed
// List(PT2S, PT1S)
```

```scala
runDemo(
  ZIO.foreachPar(Seq(2, 1)) { i =>
    sleepThenPrint(i.seconds)
  }
)
// 1 s elapsed
// 2 s elapsed
// List(PT2S, PT1S)
```


```scala
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
// 1 s elapsed
// 2 s elapsed
// ()
```

```scala
runDemo(
  defer {
    val f1 = sleepThenPrint(2.seconds).fork.run
    val f2 = sleepThenPrint(1.seconds).fork.run
    f1.join.run
    f2.join.run
  }
)
// 1 s elapsed
// 2 s elapsed
// PT1S
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/10_Concurrency_Low_Level.md)
