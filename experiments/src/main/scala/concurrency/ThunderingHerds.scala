package concurrency

import zio.*
import java.nio.file.Path

trait FileSystem

case class FileContents(contents: List[String])

trait FileService:
  def retrieveContents(name: Path): ZIO[
    FileSystem,
    Nothing,
    FileContents
  ]

object FileService:
  private def readFileExpensive(name: Path): ZIO[
    FileSystem,
    Nothing,
    FileContents
  ] =
    ZIO
      .succeed(
        FileContents(
          List("viralImage1", "viralImage2")
        )
      )
      .debug("Reading from FileSystem")
      .delay(2.seconds)

  val live =
    ZLayer.fromZIO(
      for
        accessCount <- Ref.make[Int](0)
        cache <-
          Ref.make[Map[Path, FileContents]](
            Map.empty
          )
        activeRefreshes <-
          Ref.make[Map[Path, ActiveUpdate]](
            Map.empty
          )
      yield Live(
        accessCount,
        cache,
        activeRefreshes
      )
    )

  case class ActiveUpdate(
      observers: Int,
      promise: Promise[
        Nothing,
        FileContents
      ]
  )

  case class Live(
      accessCount: Ref[
        Int
      ], // TODO Consider removing
      cache: Ref[Map[Path, FileContents]],
      activeRefresh: Ref[Map[Path, ActiveUpdate]]
  ) extends FileService:
    def retrieveContents(name: Path): ZIO[
      FileSystem,
      Nothing,
      FileContents
    ] =
      for
        cachedValue <- cache.get.map(_.get(name))
        activeValue <-
          cachedValue match
            case Some(initValue) =>
              ZIO.debug("Value was cached. Easy path.") *>
              ZIO.succeed(initValue)
            case None =>
              retrieveOrWaitForContents(name)
      yield activeValue

    private def retrieveOrWaitForContents(
        name: Path
    ) =
      for
        promiseThatMightNotBeUsed <-
          Promise
            .make[Nothing, FileContents]
        activeUpdate <-
          activeRefresh
            .updateAndGet { activeRefreshes =>
              activeRefreshes.updatedWith(name) {
                case Some(promise) =>
                  Some(
                    promise.copy(observers =
                      promise.observers + 1
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
            .map(_(name)) // TODO Unsafe/cryptic
        finalContents <-
          activeUpdate.observers match
            case 0 =>
              for
                _ <-
                  ZIO.debug(
                     "1st herd member will hit the filesystem"
                  )
                contents <-
                  readFileExpensive(name)
                _ <-
                  activeUpdate
                    .promise
                    .succeed(contents)
                _ <- activeRefresh.update(m => m - name) // Clean out "active" entry
                _ <- cache.update(m => m.updated(name, contents)) // Update cache
              yield contents
            case observerCount =>
              ZIO.debug(
                "Slower herd member will wait for response of 1st member"
              ) *>
                activeUpdate
                  .promise
                  .await
                  .debug(
                    "Slower herd member got answer from 1st member"
                  )
      yield finalContents
  end Live
end FileService

val users = List("Bill", "Bruce", "James")

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
    herdBehavior.provide(
      FileService.live,
      ZLayer.succeed(new FileSystem {})
    )
