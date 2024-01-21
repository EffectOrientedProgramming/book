# Concurrency High Level

1. forEachPar, collectAllPar

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
  ZIO.foreach(Seq(2, 1)): i =>
    sleepThenPrint(i.seconds)
// List(PT2S, PT1S)
```

```scala
runDemo:
  ZIO.foreachPar(Seq(2, 1)): i =>
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
    Console
      .printLine:
        total
      .run
// ()
```



```scala
def slowFailableRandom(duration: Duration) =
  defer:
    val randInt =
      Random.nextIntBetween(0, 100).run
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
// 14 m 55 s
```

## zipPar, zipWithPar

## validateWithPar?

## withParallelism


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/08_Concurrency.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/concurrency/Caching.scala
```scala
package concurrency

import zio.ZLayer
import zio.cache.{Cache, Lookup}

import java.nio.file.Path

// TODO Move this all to concurrency_state prose when we can bring tests over in a decent way

case class FileContents(contents: List[String])

trait PopularService:
  def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents]

trait CloudStorage:
  def expensiveDownload(
      name: Path
  ): ZIO[Any, Nothing, FileContents]

  val invoice: ZIO[Any, Nothing, String]

// Invisible
object CloudStorage:
  val live =
    ZLayer.fromZIO:
      defer:
        FSLive(Ref.make(0).run)
// /Invisible

case class ServiceUncached(files: CloudStorage)
    extends PopularService:
  override def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    files.expensiveDownload(name)

object ServiceUncached:
  val live =
    ZLayer.fromFunction:
      ServiceUncached.apply

// TODO Do we care about this level of indirection?
case class ServiceCached(
    cache: Cache[Path, Nothing, FileContents]
) extends PopularService:
  override def retrieve(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    cache.get(name)

object ServiceCached:
  val make =
    defer:
      val cache
          : Cache[Path, Nothing, FileContents] =
        Cache
          .make(
            capacity = 100,
            timeToLive = Duration.Infinity,
            lookup =
              Lookup((key: Path) =>
                ZIO.serviceWithZIO[CloudStorage]:
                  _.expensiveDownload(key)
              )
          )
          .run
      ServiceCached(cache)

```


### experiments/src/main/scala/concurrency/Invisible.scala
```scala
package concurrency

import zio.Console.printLine

import java.nio.file.Path

// TODO Figure if these functions belong in the object instead.
case class FSLive(requests: Ref[Int])
    extends CloudStorage:
  def expensiveDownload(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    defer:
      // TODO Delete this when we are confident
      // we only care about the invoice
      printLine("Reading from FileSystem")
        .orDie
        .run
      requests.update(_ + 1).run

      ZIO.sleep(2.seconds).run
      FSLive.hardcodedContents

  val invoice: ZIO[Any, Nothing, String] =
    requests
      .get
      .map(count => "Amount owed: $" + count)
end FSLive

object FSLive:
  val hardcodedContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )

```

