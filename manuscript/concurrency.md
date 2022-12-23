# Concurrency

## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/concurrency/LunchVote.scala
```scala
package concurrency

import concurrency.LunchVote.Vote.Yay
import zio.*
import zio.concurrent.*

object LunchVote extends ZIOAppDefault:

  enum Vote:
    case Yay,
      Nay

  def getVoteFrom(
      person: String,
      results: ConcurrentMap[Vote, Int],
      votingComplete: Promise[Nothing, Vote],
      voterCount: Int
  ): ZIO[Any, Nothing, Vote] =
    (
      for
        sleepAmount <-
          Random.nextIntBetween(1, 5)
        _ <- ZIO.sleep(sleepAmount.seconds)
        answer <-
          Random
            .nextBoolean
            .map(b =>
              if (b)
                Vote.Yay
              else
                Vote.Nay
            )
            .debug(s"$person vote")
        currentTally <-
          results
            .computeIfPresent(
              answer,
              (_, previous) => previous + 1
            )
            .someOrFail(
              IllegalStateException(
                "Vote not found"
              )
            )
            .orDie
        _ <-
          ZIO.when(
            currentTally > voterCount / 2
          )(votingComplete.succeed(answer))
//      _ <- votingComplete.isDone.debug(s"Voting complete when $person voted")
      yield answer
    ).onInterrupt(
      ZIO.debug(s"interrupted $person vote")
    )

  def run =
    val voters =
      List(
        "Alice",
        "Bob",
        "Charlie",
        "Dave",
        "Eve"
      )
    for
      resultMap <-
        ConcurrentMap.make[Vote, Int](
          Vote.Yay -> 0,
          Vote.Nay -> 0
        )
      votingComplete <-
        Promise.make[Nothing, Vote]
      ongoingVotes <-
        ZIO.forkAll(
          voters.map(voter =>
            getVoteFrom(
              voter,
              resultMap,
              votingComplete,
              voters.size
            )
          )
        )
      _ <- votingComplete.await.debug("Result")
//      _ <- votingComplete.isDone.debug("Done")
      _ <- ongoingVotes.interrupt
//      _ <- results.
    yield ()
    end for
  end run
end LunchVote

```


### experiments/src/main/scala/concurrency/OperatorDemos.scala
```scala
package concurrency

import zio.{
  durationInt,
  duration2DurationOps,
  Clock,
  Console,
  Duration,
  ZIO,
  ZIOAppDefault,
  Random
}

def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  for
    _ <- ZIO.sleep(d)
    _ <-
      Console.printLine(s"${d.render} elapsed")
  yield d

object ForkDemo extends zio.ZIOAppDefault:
  override def run =
    for
      f1 <- sleepThenPrint(2.seconds).fork
      f2 <- sleepThenPrint(1.seconds).fork
      _  <- f1.join
      _  <- f2.join
    yield ()

object ForEachDemo extends zio.ZIOAppDefault:
  override def run =
    ZIO.foreach(Seq(2, 1)) { i =>
      sleepThenPrint(i.seconds)
    }

object ForEachParDemo extends zio.ZIOAppDefault:
  override def run =
    ZIO.foreachPar(Seq(2, 1)) { i =>
      sleepThenPrint(i.seconds)
    }

object RaceDemo extends zio.ZIOAppDefault:
  override def run =
    ZIO.raceAll(
      sleepThenPrint(2.seconds),
      Seq(sleepThenPrint(1.seconds))
    )
    /* // alternate syntax:
     * sleepThenPrint(2.seconds).race(Seq(sleepThenPrint(1.seconds)) */

object CollectAllParDemo
    extends zio.ZIOAppDefault:
  override def run =
    for
      durations <-
        ZIO.collectAllPar(
          Seq(
            sleepThenPrint(2.seconds),
            sleepThenPrint(1.seconds)
          )
        )
      total =
        durations
          .fold(Duration.Zero)(_ + _)
          .render
      _ <- Console.printLine(total)
    yield ()
end CollectAllParDemo

object CollectAllParMassiveDemo
    extends zio.ZIOAppDefault:
  override def run =
    for
      durations <-
        ZIO.collectAllSuccessesPar(
          Seq
            .fill(1_000_000)(1.seconds)
            .map(duration =>
              for
                randInt <-
                  Random.nextIntBetween(0, 100)
                _ <- ZIO.sleep(duration)
                _ <-
                  ZIO.when(randInt < 10)(
                    ZIO.fail("Number is too low")
                  )
              yield duration
            )
        )
      total =
        durations
          .fold(Duration.Zero)(_ + _)
          .render
      _ <- Console.printLine(total)
    yield ()
end CollectAllParMassiveDemo

```

            