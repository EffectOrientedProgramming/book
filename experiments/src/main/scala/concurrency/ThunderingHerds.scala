package concurrency

import zio.*

trait FileSystem
trait FileNotFound
trait RetrievalFailure

trait FileService:
    def retrieveContents(name: String): ZIO[FileSystem, RetrievalFailure | FileNotFound, List[String]]

object FileService:
    def readFile(name: String): ZIO[FileSystem, RetrievalFailure | FileNotFound, List[String]] =
      ZIO.succeed(
        List("viralImage1", "viralImage2")
      ).debug("Reading from FileSystem").delay(2.seconds)

    val live =
      ZLayer.fromZIO(
        for
          accessCount <- Ref.make[Int](0)
          cache <- Ref.make[Map[String, List[String]]](Map.empty)
          activeRefreshes <- Ref.Synchronized.make[Map[String, Promise[RetrievalFailure, List[String]]]](Map.empty)
        yield Live(accessCount, cache, activeRefreshes)
      )

    case class Live(
        accessCount: Ref[Int],
        cache: Ref[Map[String, List[String]]],
        activeRefresh: Ref.Synchronized[Map[String, Promise[RetrievalFailure, List[String]]]]
        ) extends FileService:
            def retrieveContents(name: String): ZIO[FileSystem, RetrievalFailure | FileNotFound, List[String]] =
                for
                  currentCache <- cache.get
                  initialValue = currentCache.get(name)
                  activeValue <-
                    initialValue match
                      case Some(initValue) =>
                        ZIO.succeed(initValue)
                      case None =>
                          retrieveOrWaitForContents(name)
                yield activeValue

            enum RefreshState:
              case NewlyActive, AlreadyActive

            def retrieveOrWaitForContents(name: String) =
              for
                state <- Promise.make[Nothing, RefreshState]
                promise <- activeRefresh.updateAndGetZIO { activeRefreshes =>
                  activeRefreshes.get(name) match
                    case Some(promise) =>
                        state.succeed(RefreshState.AlreadyActive) *>
                          ZIO.succeed(activeRefreshes)
                    case None =>
                      for
                        promise <- Promise.make[RetrievalFailure, List[String]]
                        _ <- state.succeed(RefreshState.NewlyActive)
                      yield activeRefreshes + (name -> promise)
                }.map(_(name)) // TODO Unsafe/cryptic
                finalStatus <- state.await
                finalContents <- finalStatus match
                        case RefreshState.NewlyActive =>
                          for
                            _ <- ZIO.debug("1st herd member is going to hit the filesystem")
                            contents <- readFile(name)
                            _ <- promise.succeed(contents)
                          yield contents
                        case RefreshState.AlreadyActive =>
                            ZIO.debug("Slower herd member is going to wait for the response of 1st member") *>
                              promise.await.debug("Slower herd member got answer from 1st member")


              yield finalContents

val users = List(
  "Bill",
  "Bruce",
  "James",
)

val herdBehavior =
  for
    fileService <- ZIO.service[FileService]
    _ <- ZIO.foreachParDiscard(users)(user =>
      fileService.retrieveContents("awesomeMemes")
    )
  yield ()

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior.provide(FileService.live, ZLayer.succeed(new FileSystem {}))