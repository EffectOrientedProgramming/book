package concurrency

import zio.ZLayer
import zio.cache.{Cache, Lookup}

import java.nio.file.Path

// TODO Move this all to concurrency_state prose when we can bring tests over in a decent way

case class FileContents(contents: List[String])

trait PopularService:
  def retrieveContents(
      name: Path
  ): ZIO[Any, Nothing, FileContents]

  val misses: ZIO[Any, Nothing, Int]

trait FileSystem:
  def readFileExpensive(
                         name: Path
                       ): ZIO[Any, Nothing, FileContents]

object FileSystem:
  val live = ZLayer.succeed(FSLive())

case class NoCacheAtAll(
    fileSystem: FileSystem,
    missesRef: Ref[Int]
                       ) extends PopularService:
  override def retrieveContents(
                                 name: Path
                               ): ZIO[Any, Nothing, FileContents] =
    defer:
      missesRef.update(_+1).run
      fileSystem.readFileExpensive(name).run

  override val misses: ZIO[Any, Nothing, Int] =
    missesRef.get

object NoCacheAtAll:
  val live =
    ZLayer.fromZIO:
      defer:
        NoCacheAtAll(
          ZIO.service[FileSystem].run,
          Ref.make(0).run
        )

case class ServiceThatCanHandleThunderingHerds(
    cache: Cache[Path, Nothing, FileContents]
) extends PopularService:
  override def retrieveContents(
      name: Path
  ): ZIO[Any, Nothing, FileContents] =
    cache.get(name)

  override val misses: ZIO[Any, Nothing, Int] =
    defer:
      cache.cacheStats.run.misses.toInt

object ServiceThatCanHandleThunderingHerds:
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
      ServiceThatCanHandleThunderingHerds(cache)
end ServiceThatCanHandleThunderingHerds
