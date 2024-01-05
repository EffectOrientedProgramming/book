# Concurrency Low Level

1. Fork join
1. Throwaway reference to STM

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
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/14_ForkJoin.md)
