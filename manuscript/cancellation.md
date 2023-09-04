## cancellation

 

### experiments/src/main/scala/cancellation/CancellingATightLoop.scala
```scala
package cancellation

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.text.similarity.LevenshteinDistance

val input  = RandomStringUtils.random(70_000)
val target = RandomStringUtils.random(70_000)
val leven =
  LevenshteinDistance.getDefaultInstance

object PlainLeven extends App:
  leven(input, target)

object CancellingATightLoop
    extends ZIOAppDefault:
  val scenario =
    ZIO
      .attempt(leven(input, target))
      .mapBoth(
        error => ZIO.succeed("yay!"),
        success => ZIO.fail("Oh no!")
      )

  def run =
    // For timeouts, you need fibers and
    // cancellation
    scenario
      // TODO This is running for 16 seconds
      // nomatter what.
      .timeout(1.seconds).timed.debug("Time:")

```


### experiments/src/main/scala/cancellation/FutureCancellation.scala
```scala
package cancellation

import scala.concurrent.Future

// We show that Future's are killed with finalizers that never run
object FutureNaiveCancellation
    extends ZIOAppDefault:

  def run =
    ZIO
      .fromFuture:
        Future:
          try Thread.sleep(500)
          finally println("Cleanup")
          "Success!"
      .timeout(25.millis)
      .debug

```


### experiments/src/main/scala/cancellation/HelloCancellation.scala
```scala
package cancellation

val longRunning =
  defer:
    ZIO.debug("  Started longrunning").run
    ZIO.sleep(5.seconds).run
    ZIO.debug("  Finished longrunning").run
  .onInterrupt(
    ZIO.debug("Interrupted LongRunning")
  )

object HelloCancellation extends ZIOAppDefault:

  def run = longRunning.timeout(2.seconds)

object HelloCancellation2 extends ZIOAppDefault:
  val complex =
    defer:
      ZIO.debug("starting complex operation").run
      longRunning.run
      ZIO
        .debug("finishing complex operation")
        .run
    .onInterrupt(
      ZIO.debug("Interrupted Complex")
    )

  def run = complex.timeout(2.seconds)

object CancellationWeb extends ZIOAppDefault:

  def spawnOperations(
      level: Int,
      limit: Int,
      parent: String
  ): ZIO[Any, Nothing, Unit] =
    val indent = " " * (level * 2)
    if (level < limit)
      defer:
        ZIO
          .debug(indent + parent + " started")
          .run
        ZIO.sleep(level.seconds).run

        ZIO
          .foreachPar(List("L", "R"))(label =>
            spawnOperations(
              level + 1,
              limit,
              parent + s"-$label"
            )
          )
          .run

        ZIO
          .debug(indent + parent + " finished")
          .run
      .onInterrupt(
        ZIO.debug(
          indent + parent + " interrupted"
        )
      ).unit
    else
      ZIO.unit
  end spawnOperations

  def run =
    spawnOperations(0, 3, "Root")
      .timeout(3.seconds)
end CancellationWeb

object FailureDuringFork extends ZIOAppDefault:
  def run =
    defer {
      val fiber1 =
        defer:
          ZIO.sleep(5.seconds).run
          ZIO.debug("Complete fiber 1").run
        .onInterrupt(
          ZIO.debug("Interrupted fiber 1")
        ).fork
          .run

      val fiber2 =
        defer:
          ZIO.sleep(5.seconds).run
          ZIO.debug("Complete fiber 2").run
        .onInterrupt(
          ZIO.debug("Interrupted fiber 2")
        ).fork
          .run

      // Once we fail here, the fibers will be
      // interrupted.
      ZIO.fail("Youch!").run
      fiber1.join.run
      fiber2.join.run
    }
end FailureDuringFork

```


