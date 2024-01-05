# Concurrency Low Level

1. Fork join
1. Throwaway reference to STM

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


ZIO is based on the fork/join model of concurrency, utilizing fibers for computations.
Most of the time, you will not need to create fibers directly.
ZIO's operators will create and utilize fibers behind the scenes, and you will not need to worry about them.

Fibers are lightweight threads that are scheduled by ZIO onto a thread pool.
Fibers are not operating system threads, and they are not scheduled by the operating system.
Instead, they are scheduled by ZIO onto a thread pool.
This means that ZIO fibers are extremely cheap to create and use, and they can be used to model even very large numbers of concurrent computations.

If a fiber is forked, and not explicitly joined, it will be interrupted when the parent fiber terminates.
If you want a fiber to exist beyond the lifetime of its parent, you must `forkDaemon` it.
