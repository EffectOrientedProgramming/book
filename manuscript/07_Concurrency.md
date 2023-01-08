# Concurrency

TODO Prose

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

object LunchVote:

  enum Vote:
    case Yay,
      Nay

  case class Voter(
      name: String,
      delay: Duration,
      response: Vote,
      onInterrupt: ZIO[Any, Nothing, Unit] =
        ZIO.unit
  )

  def run(
      voters: List[Voter],
      maximumVoteTime: Duration =
        Duration.Infinity
  ) =
    for
      resultMap <-
        ConcurrentMap.make[Vote, Int](
          Vote.Yay -> 0,
          Vote.Nay -> 0
        )
      voteProcesses =
        voters.map(voter =>
          getVoteFrom(
            voter,
            resultMap,
            voters.size
          ).onInterrupt(voter.onInterrupt)
        )
      result <-
        ZIO
          .raceAll(
            voteProcesses.head,
            voteProcesses.tail
          )
          .timeout(maximumVoteTime)
          .some
    yield result
    end for
  end run

  case object NotConclusive

  def getVoteFrom(
      person: Voter,
      results: ConcurrentMap[Vote, Int],
      voterCount: Int
  ): ZIO[Any, NotConclusive.type, Vote] =
    for
      _ <- ZIO.sleep(person.delay)
      answer = person.response
      currentTally <-
        results
          .computeIfPresent(
            answer,
            (key, previous) => previous + 1
          )
          .someOrFail(
            IllegalStateException(
              "Vote not found"
            )
          )
          .orDie
      _ <-
        ZIO.when(currentTally <= voterCount / 2)(
          ZIO.fail(NotConclusive)
        )
    yield answer

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


### experiments/src/main/scala/concurrency/ThunderingHerds.scala
```scala
package concurrency

import zio.*

trait FileSystem
trait FileNotFound
trait RetrievalFailure

trait FileService:
  def retrieveContents(name: String): ZIO[
    FileSystem,
    RetrievalFailure | FileNotFound,
    List[String]
  ]

object FileService:
  def readFile(name: String): ZIO[
    FileSystem,
    RetrievalFailure | FileNotFound,
    List[String]
  ] =
    ZIO
      .succeed(List("line1", "line2"))
      .debug("Read from FileSystem")
      .delay(2.seconds)

  val live =
    ZLayer.fromZIO(
      for
        accessCount <- Ref.make[Int](0)
        cache <-
          Ref.make[Map[String, List[String]]](
            Map.empty
          )
        activeRefreshes <-
          Ref
            .Synchronized
            .make[Map[String, Promise[
              RetrievalFailure,
              List[String]
            ]]](Map.empty)
      yield Live(
        accessCount,
        cache,
        activeRefreshes
      )
    )

  case class Live(
      accessCount: Ref[Int],
      cache: Ref[Map[String, List[String]]],
      activeRefresh: Ref.Synchronized[Map[
        String,
        Promise[RetrievalFailure, List[String]]
      ]]
  ) extends FileService:
    def retrieveContents(name: String): ZIO[
      FileSystem,
      RetrievalFailure | FileNotFound,
      List[String]
    ] =
      for
        currentCache <- cache.get
        initialValue = currentCache.get(name)
        activeValue <-
          initialValue match
            case Some(initValue) =>
              ZIO.succeed(initValue)
            case None =>
              retrieveOrWaitForContents(name)
      yield activeValue

    enum RefreshState:
      case NewlyActive,
        AlreadyActive

    def retrieveOrWaitForContents(name: String) =
      for
        state <-
          Promise.make[Nothing, RefreshState]
        promise <-
          activeRefresh
            .updateAndGetZIO { activeRefreshes =>
              activeRefreshes.get(name) match
                case Some(promise) =>
                  state.succeed(
                    RefreshState.AlreadyActive
                  ) *>
                    ZIO.succeed(activeRefreshes)
                case None =>
                  for
                    promise <-
                      Promise.make[
                        RetrievalFailure,
                        List[String]
                      ]
                    _ <-
                      state.succeed(
                        RefreshState.NewlyActive
                      )
                  yield activeRefreshes +
                    (name -> promise)
            }
            .map(_(name)) // TODO Unsafe
        finalStatus <- state.await
        finalContents <-
          finalStatus match
            case RefreshState.NewlyActive =>
              for
                contents <- readFile(name)
                _ <- promise.succeed(contents)
              yield contents
            case RefreshState.AlreadyActive =>
              promise
                .await
                .debug(
                  "Got contents from promise completed by someone else"
                )
      yield finalContents
  end Live
end FileService

val users = List("Bill", "Bruce", "James")

val herdBehavior =
  for
    fileService <- ZIO.service[FileService]
    _ <-
      ZIO.foreachPar(users)(user =>
        ZIO.debug(
          s"$user wants to see popular file"
        )
        fileService
          .retrieveContents("awesomeMemes")
      )
  yield ()

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior.provide(
      FileService.live,
      ZLayer.succeed(new FileSystem {})
    )

```

            