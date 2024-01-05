# Concurrency Fork Join

Programs that need to perform multiple operations at the same time may need to utilize a technique called "fork/join" where 
operations are explicitly "forked" creating two parallel contexts of execution.  
Usually fork is used so that overall progress of multiple operations can happen faster than if they were done sequentially.  

If we want to ensure the fork completes, then a join is used to bring the two parallel operations back together.

An example operation that makes it easy to visualize fork/join is one that sleeps for some amount of time, 
then prints how long it actually slept for, and then returns that elapsed time:
```scala mdoc
def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  defer {
    val elapsed = ZIO.sleep(d).timed.run
    Console.println(s"${elapsed.render} elapsed").run
    elapsed
  }
```

With ZIO we can fork the `sleepThenPrint` effect with two durations and verify that they in-fact run in parallel as 
the shorter duration that is forked after the longer one, prints before the longer one:
```scala mdoc
runDemo(
  ZIO.foreachPar(List(2.seconds, 1.seconds)):
    sleepThenPrint
    
  defer {
    val f1 = sleepThenPrint(2.seconds).fork.run
    val f2 = sleepThenPrint(1.seconds).fork.run
    f1.join.run
    f2.join.run
  }
)
```

The joins cause the main execution to "wait" until both effects have completed before continuing.

A variation of fork that can be used for operations which do not need to be joined is `forkDaemon` which is typically used for long-running background processes.

Bill:

ZIO is based on the fork/join model of concurrency, utilizing fibers for computations.
Most of the time, you will not need to create fibers directly.
ZIO's operators will create and utilize fibers behind the scenes, and you will not need to worry about them.

Fibers are lightweight threads that are scheduled by ZIO onto a thread pool.
Fibers are not operating system threads, and they are not scheduled by the operating system.
Instead, they are scheduled by ZIO onto a thread pool.
This means that ZIO fibers are extremely cheap to create and use, and they can be used to model even very large numbers of concurrent computations.

If a fiber is forked, and not explicitly joined, it will be interrupted when the parent fiber terminates.
If you want a fiber to exist beyond the lifetime of its parent, you must `forkDaemon` it.
