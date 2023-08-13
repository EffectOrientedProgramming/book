# Concurrency

TODO Prose

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/07_Concurrency.md)


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
    defer {
      val resultMap =
        ConcurrentMap
          .make[Vote, Int](
            Vote.Yay -> 0,
            Vote.Nay -> 0
          )
          .run
      val voteProcesses =
        voters.map(voter =>
          getVoteFrom(
            voter,
            resultMap,
            voters.size
          ).onInterrupt(voter.onInterrupt)
        )
      ZIO
        .raceAll(
          voteProcesses.head,
          voteProcesses.tail
        )
        .timeout(maximumVoteTime)
        .some
        .run
    }
  end run

  case object NotConclusive

  def getVoteFrom(
      person: Voter,
      results: ConcurrentMap[Vote, Int],
      voterCount: Int
  ): ZIO[Any, NotConclusive.type, Vote] =
    defer {
      ZIO.sleep(person.delay).run
      val answer = person.response
      val currentTally =
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
          .run
      ZIO
        .when(currentTally <= voterCount / 2)(
          ZIO.fail(NotConclusive)
        )
        .run
      answer
    }

end LunchVote

```


### experiments/src/main/scala/concurrency/OperatorDemos.scala
```scala
package concurrency

def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  defer {
    ZIO.sleep(d).run
    Console.printLine(s"${d.render} elapsed").run
    d
  }

object ForkDemo extends zio.ZIOAppDefault:
  override def run =
    defer {
      val f1 = sleepThenPrint(2.seconds).fork.run
      val f2 = sleepThenPrint(1.seconds).fork.run
      f1.join.run
      f2.join.run
    }

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
        durations
          .fold(Duration.Zero)(_ + _)
          .render
      Console.printLine(total).run
    }
end CollectAllParDemo

object CollectAllParMassiveDemo
    extends zio.ZIOAppDefault:
  override def run =
    defer {
      val durations =
        ZIO
          .collectAllSuccessesPar(
            Seq
              .fill(1_000_000)(1.seconds)
              .map(duration =>
                defer {
                  val randInt =
                    Random
                      .nextIntBetween(0, 100)
                      .run
                  ZIO.sleep(duration).run
                  ZIO
                    .when(randInt < 10)(
                      ZIO.fail(
                        "Number is too low"
                      )
                    )
                    .run
                  duration
                }
              )
          )
          .run
      val total =
        durations
          .fold(Duration.Zero)(_ + _)
          .render
      Console.printLine(total).run
    }

end CollectAllParMassiveDemo

```


### experiments/src/main/scala/concurrency/ThunderingHerds.scala
```scala
package concurrency

import concurrency.FileService.ActiveUpdate
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
      defer {
        val fs   = ZIO.service[FileSystem].run
        val hit  = Ref.make[Int](0).run
        val miss = Ref.make[Int](0).run
        val cache =
          Ref
            .make[Map[Path, FileContents]](
              Map.empty
            )
            .run
        val activeRefreshes =
          Ref
            .make[Map[Path, ActiveUpdate]](
              Map.empty
            )
            .run
        Live(
          hit,
          miss,
          cache,
          activeRefreshes,
          fs
        )
      }
    )

  case class ActiveUpdate(
      observers: Int,
      promise: Promise[Nothing, FileContents]
  ):
    def completeWith(contents: FileContents) =
      promise.succeed(contents)

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
      defer {
        val cachedValue =
          cache.get.map(_.get(name)).run
        val activeValue =
          cachedValue match
            case Some(initValue) =>
              (
                hit.update(_ + 1) *>
                  printLine(
                    "Value was cached. Easy path."
                  ).orDie *>
                  ZIO.succeed(initValue)
              ).run
            case None =>
              retrieveOrWaitForContents(name).run
        activeValue
      }

    private def retrieveOrWaitForContents(
        name: Path
    ) =
      defer {
        val promiseThatMightNotBeUsed =
          Promise.make[Nothing, FileContents].run
        val activeUpdates =
          calculateActiveUpdates(
            activeRefresh,
            name,
            promiseThatMightNotBeUsed
          ).run
        val activeUpdate = activeUpdates(name)
        val finalContents =
          activeUpdate.observers match
            case 0 =>
              firstHerdMemberBehavior(
                fileSystem,
                activeUpdate,
                activeRefresh,
                miss,
                cache,
                name
              ).run
            case observerCount =>
              slowHerdMemberBehavior(
                hit,
                activeUpdate
              ).run
        finalContents
      }

    val hits: ZIO[Any, Nothing, Int] = hit.get

    val misses: ZIO[Any, Nothing, Int] = miss.get

  end Live
end FileService

def slowHerdMemberBehavior(
    hit: Ref[Int],
    activeUpdate: ActiveUpdate
) =
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

def calculateActiveUpdates(
    activeRefresh: Ref[Map[Path, ActiveUpdate]],
    name: Path,
    promiseThatMightNotBeUsed: Promise[
      Nothing,
      FileContents
    ]
) =
  activeRefresh.updateAndGet { activeRefreshes =>
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

def firstHerdMemberBehavior(
    fileSystem: FileSystem,
    activeUpdate: ActiveUpdate,
    activeRefresh: Ref[Map[Path, ActiveUpdate]],
    miss: Ref[Int],
    // TODO Consider ConcurrentMap
    cache: Ref[Map[Path, FileContents]],
    name: Path
) =
  defer {
    printLine(
      "1st herd member will hit the filesystem"
    ).orDie.run
    val contents =
      fileSystem.readFileExpensive(name).run
    activeUpdate.completeWith(contents).run

    activeRefresh
      .update(m =>
        m - name // Clean out "active" entry
      )
      .run
    cache
      .update(m =>
        m.updated(name, contents) // Update cache
      )
      .run
    miss.update(_ + 1).run
    contents
  }

val users = (0 to 1000).toList.map("User " + _)
//  List("Bill", "Bruce", "James")

val herdBehavior =
  defer {
    val fileService =
      ZIO.service[FileService].run
    ZIO
      .foreachParDiscard(users)(user =>
        fileService.retrieveContents(
          Path.of("awesomeMemes")
        )
      )
      .run
    ZIO.debug("=========").run
    fileService
      .retrieveContents(Path.of("awesomeMemes"))
      .run
  }

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
    defer {
      cache.cacheStats.run.hits.toInt
    }
  override val misses: ZIO[Any, Nothing, Int] =
    defer {
      cache.cacheStats.run.misses.toInt
    }

object ThunderingHerdsUsingZioCacheLib:
  val make =
    defer {
      val retrievalFunction =
        ZIO
          .service[FileSystem]
          .map(_.readFileExpensive)
          .run
      val cache
          : Cache[Path, Nothing, FileContents] =
        Cache
          .make(
            capacity = 100,
            timeToLive = Duration.Infinity,
            lookup = Lookup(retrievalFunction)
          )
          .run
      ThunderingHerdsUsingZioCacheLib(cache)
    }
end ThunderingHerdsUsingZioCacheLib

```


### experiments/src/main/scala/concurrency/WhyZio.scala
```scala
package concurrency

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

