package concurrency

import zio.cache.{Cache, Lookup}

import java.nio.file.Path
import zio.Console.printLine

// TODO Move this all to concurrency_state prose when we can bring tests over in a decent way

case class FileContents(contents: List[String])

trait FileService:
  def retrieveContents(
                        name: Path
                      ): ZIO[Any, Nothing, FileContents]

  val hits: ZIO[Any, Nothing, Int]

  val misses: ZIO[Any, Nothing, Int]


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

  def readFileExpensive2(
                          name: Path
                        ): ZIO[Any, Nothing, FileContents] =
    defer:
      printLine("Reading from FileSystem")
        .orDie
        .run

      ZIO.sleep(2.seconds).run
      FileSystem.hardcodedFileContents

object FileSystem:
  val hardcodedFileContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
  val live = ZLayer.succeed(new FileSystem {})

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
    defer:
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
end ThunderingHerdsUsingZioCacheLib
