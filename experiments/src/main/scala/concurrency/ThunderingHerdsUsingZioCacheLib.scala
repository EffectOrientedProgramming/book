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
