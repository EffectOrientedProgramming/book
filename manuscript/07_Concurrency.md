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
import zio.Console.printLine

import java.nio.file.Path

case class FileContents(contents: List[String])

trait FileService:
  def retrieveContents(
      name: Path
  ): ZIO[Any, Nothing, FileContents]

  val hits: ZIO[Any, Nothing, Int]

  val misses: ZIO[Any, Nothing, Int]

object FileService:
  val live =
    ZLayer.fromZIO(
      for
        fs   <- ZIO.service[FileSystem]
        hit  <- Ref.make[Int](0)
        miss <- Ref.make[Int](0)
        cache <-
          Ref.make[Map[Path, FileContents]](
            Map.empty
          )
        activeRefreshes <-
          Ref.make[Map[Path, ActiveUpdate]](
            Map.empty
          )
      yield Live(
        hit,
        miss,
        cache,
        activeRefreshes,
        fs
      )
    )

  case class ActiveUpdate(
      observers: Int,
      promise: Promise[Nothing, FileContents]
  )

  case class Live(
      hit: Ref[Int],
      miss: Ref[Int],
      // TODO Consider ConcurrentMap
      cache: Ref[Map[Path, FileContents]],
      activeRefresh: Ref[
        Map[Path, ActiveUpdate]
      ],
      fileSystem: FileSystem
  ) extends FileService:

    def retrieveContents(
        name: Path
    ): ZIO[Any, Nothing, FileContents] =
      for
        cachedValue <- cache.get.map(_.get(name))
        activeValue <-
          cachedValue match
            case Some(initValue) =>
              hit.update(_ + 1) *>
                printLine(
                  "Value was cached. Easy path."
                ).orDie *> ZIO.succeed(initValue)
            case None =>
              retrieveOrWaitForContents(name)
      yield activeValue

    private def retrieveOrWaitForContents(
        name: Path
    ) =
      for
        promiseThatMightNotBeUsed <-
          Promise.make[Nothing, FileContents]
        activeUpdates <-
          activeRefresh.updateAndGet {
            activeRefreshes =>
              activeRefreshes.updatedWith(name) {
                case Some(activeUpdate) =>
                  Some(
                    activeUpdate.copy(observers =
                      activeUpdate.observers + 1
                    )
                  )
                case None =>
                  Some(
                    ActiveUpdate(
                      0,
                      promiseThatMightNotBeUsed
                    )
                  )
              }
          }

        activeUpdate = activeUpdates(name)
        finalContents <-
          activeUpdate.observers match
            case 0 =>
              for
                _ <-
                  printLine(
                    "1st herd member will hit the filesystem"
                  ).orDie
                contents <-
                  fileSystem
                    .readFileExpensive(name)
                _ <-
                  activeUpdate
                    .promise
                    .succeed(contents)
                _ <-
                  activeRefresh.update(m =>
                    m - name
                  ) // Clean out "active" entry
                _ <-
                  cache.update(m =>
                    m.updated(name, contents)
                  ) // Update cache
                _ <- miss.update(_ + 1)
              yield contents
            case observerCount =>
              printLine(
                "Slower herd member will wait for response of 1st member"
              ).orDie *> hit.update(_ + 1) *>
                activeUpdate
                  .promise
                  .await
                  .tap(_ =>
                    printLine(
                      "Slower herd member got answer from 1st member"
                    ).orDie
                  )
      yield finalContents

    val hits: ZIO[Any, Nothing, Int] = hit.get

    val misses: ZIO[Any, Nothing, Int] = miss.get

  end Live
end FileService

val users = (0 to 1000).toList.map("User " + _)
//  List("Bill", "Bruce", "James")

val herdBehavior =
  for
    fileService <- ZIO.service[FileService]
    _ <-
      ZIO.foreachParDiscard(users)(user =>
        fileService.retrieveContents(
          Path.of("awesomeMemes")
        )
      )
    _ <- ZIO.debug("=========")
    _ <-
      fileService.retrieveContents(
        Path.of("awesomeMemes")
      )
  yield ()

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior
      .provide(FileSystem.live, FileService.live)

trait FileSystem:
  def readFileExpensive(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    ZIO
      .succeed(FileSystem.hardcodedFileContents)
      .tap(_ =>
        printLine("Reading from FileSystem")
          .orDie
      )
      .delay(2.seconds)

object FileSystem:
  val hardcodedFileContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
  val live = ZLayer.succeed(new FileSystem {})

```


### experiments/src/main/scala/concurrency/ThunderingHerdsUsingZioCacheLib.scala
```scala
package concurrency

import zio.*
import zio.cache.{Cache, Lookup}

import java.nio.file.Path

case class ThunderingHerdsUsingZioCacheLib(
    cache: Cache[Path, Nothing, FileContents]
) extends FileService:
  override def retrieveContents(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    cache.get(name)

  override val hits: ZIO[Any, Nothing, Int] =
    for stats <- cache.cacheStats
    yield stats.hits.toInt
  override val misses: ZIO[Any, Nothing, Int] =
    for stats <- cache.cacheStats
    yield stats.misses.toInt

object ThunderingHerdsUsingZioCacheLib:
  val make =
    for
      retrievalFunction <-
        ZIO
          .service[FileSystem]
          .map(_.readFileExpensive)
      cache: Cache[
        Path,
        Nothing,
        FileContents
      ] <-
        Cache.make(
          capacity = 100,
          timeToLive = Duration.Infinity,
          lookup = Lookup(retrievalFunction)
        )
    yield ThunderingHerdsUsingZioCacheLib(cache)

```


### experiments/src/main/scala/concurrency/WhyZio.scala
```scala
package concurrency

import zio.{ZIO, ZIOAppDefault}

import java.math.BigInteger

object WhyZio extends ZIOAppDefault:

  override def run =
    val genPrime =
      ZIO
        .attempt {
          crypto.nextPrimeAfter(100_000_000)
        }
        .timed

    ZIO.raceAll(genPrime, Seq(genPrime)).debug

```

