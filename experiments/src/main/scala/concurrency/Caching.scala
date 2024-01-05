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

case class ServiceUncached(
                            files: CloudStorage,
                          ) extends PopularService:
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
            lookup = Lookup(
              (key: Path) =>
                ZIO
                  .serviceWithZIO[CloudStorage]:
                    _.expensiveDownload(key)
            )
          )
          .run
      ServiceCached(cache)
end ServiceCached
