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
