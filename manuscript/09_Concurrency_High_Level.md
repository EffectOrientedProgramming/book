# Concurrency

TODO Prose
```scala
def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  defer:
    ZIO.sleep(d).run
    ZIO.debug(s"${d.render} elapsed").run
    d
```

```scala
runDemo:
  ZIO.foreach(Seq(2, 1)): 
    i =>
      sleepThenPrint(i.seconds)
// List(PT2S, PT1S)
```

```scala
runDemo:
  ZIO.foreachPar(Seq(2, 1)): 
    i =>
      sleepThenPrint(i.seconds)
// List(PT2S, PT1S)
```


```scala
runDemo:
  defer:
    val durations =
      ZIO
        .collectAllPar:
          Seq(
            sleepThenPrint(2.seconds),
            sleepThenPrint(1.seconds)
          )
        .run
    val total =
      durations.fold(Duration.Zero)(_ + _).render
    Console.printLine:
      total
    .run
// ()
```



```scala
def slowFailableRandom(duration: Duration) =
  defer:
    val randInt =
      Random
        .nextIntBetween(0, 100)
        .run
    ZIO.sleep(duration).run
    ZIO
      .when(randInt < 10)(
        ZIO.fail("Number is too low")
      )
      .run
    duration
    
// Massive example
runDemo:
  defer:
    val durations =
      ZIO
        .collectAllSuccessesPar:
          Seq
            .fill(1_000)(1.seconds)
            .map(duration =>
              slowFailableRandom(duration)
            )
        .run
    durations.fold(Duration.Zero)(_ + _).render
// 15 m 6 s
```

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/09_Concurrency_High_Level.md)
