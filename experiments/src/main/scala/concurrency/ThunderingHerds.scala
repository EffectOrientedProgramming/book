package concurrency

import zio._

trait FileService:
    def retrieveContents(name: String): ZIO[Any, Nothing, List[String]]

object FileService:
    trait FileSystem
    trait FileNotFound
    def readFile(name: String): ZIO[FileSystem, FileNotFound, List[String]] = ???

    trait RetrievalFailure
    case class Live(
        accessCount: Ref[Int],
        cache: Ref[Map[String, List[String]]],
        activeRefresh: Ref[Map[String, Promise[RetrievalFailure, List[String]]]]
        ) extends FileService:
            def retrieveContents(name: String): ZIO[Any, Nothing, List[String]] =
                for
                  initialValue <- cache.get(name)
                  activeValue <-
                    ZIO.when(initialValue.isEmpty)
                      activeRefresh(name) match
                        case Some(pendingValue) =>
                           pendingValue.await
                        case None =>
                           for
                             refreshAttempt <- Promise.make[RetrievalFailure, List[String]]
                             _ <- activeRefresh.update(_.set(name, refreshAttempt))
                             contentsFromDisk <- readFile(name)
                             _ <- refreshAttempt.succeed(contentsFromDisk)
                           yield contentsFromDisk
                yield activeValue




val users = List(
  "Bill",
  "Bruce",
  "James"
)

val herdBehavior =
    ZIO.foreachPar(users)(retrieveContents("awesomeMemes"))

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior