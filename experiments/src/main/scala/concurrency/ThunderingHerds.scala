package concurrency

import zio.*
import java.nio.file.Path

trait FileSystem
trait FileNotFound
trait RetrievalFailure

case class FileContents(contents: List[String])

trait FileService:
  def retrieveContents(name: Path): ZIO[
    FileSystem,
    RetrievalFailure | FileNotFound,
    FileContents
  ]

object FileService:
  private def readFileExpensive(name: Path): ZIO[
    FileSystem,
    RetrievalFailure | FileNotFound,
    FileContents
  ] =
    ZIO
      .succeed(
        FileContents(List("viralImage1", "viralImage2"))
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
          Ref
            .Synchronized
            .make[Map[Path, Promise[
              RetrievalFailure,
              FileContents
            ]]](Map.empty)
      yield Live(
        accessCount,
        cache,
        activeRefreshes
      )
    )


  case class Live(
      accessCount: Ref[Int], // TODO Consider removing
      cache: Ref[Map[Path, FileContents]],
      activeRefresh: Ref.Synchronized[Map[
        Path,
        Promise[RetrievalFailure, FileContents]
      ]]
  ) extends FileService:
    def retrieveContents(name: Path): ZIO[
      FileSystem,
      RetrievalFailure | FileNotFound,
      FileContents
    ] =
      for
        cachedValue <- cache.get.map(_.get(name))
        activeValue <-
          cachedValue match
            case Some(initValue) =>
              ZIO.succeed(initValue)
            case None =>
              retrieveOrWaitForContents(name)
      yield activeValue

    enum RefreshState:
      case NewlyActive,
        AlreadyActive

    def retrieveOrWaitForContents(name: Path) =
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
                        FileContents
                      ]
                    _ <-
                      state.succeed(
                        RefreshState.NewlyActive
                      )
                  yield activeRefreshes +
                    (name -> promise)
            }
            .map(_(name)) // TODO Unsafe/cryptic
        finalStatus <- state.await
        finalContents <-
          finalStatus match
            case RefreshState.NewlyActive =>
              for
                _ <-
                  ZIO.debug(
                    "1st herd member is going to hit the filesystem"
                  )
                contents <- readFileExpensive(name)
                _ <- promise.succeed(contents)
              yield contents
            case RefreshState.AlreadyActive =>
              ZIO.debug(
                "Slower herd member is going to wait for the response of 1st member"
              ) *>
                promise
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
        fileService
          .retrieveContents(Path.of("awesomeMemes"))
      )
  yield ()

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior.provide(
      FileService.live,
      ZLayer.succeed(new FileSystem {})
    )
