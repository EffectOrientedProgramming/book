package concurrency

import concurrency.FileService.ActiveUpdate
import zio.Console.printLine

import java.nio.file.Path

case class FileContents(contents: List[String])

trait FileService:
  def retrieveContents(
      name: Path
  ): ZIO[Any, Nothing, FileContents]

  val hits: ZIO[Any, Nothing, Int]

  val misses: ZIO[Any, Nothing, Int]

case class FileCache(
    map: Ref[Map[Path, FileContents]]
):
  def saveContents(
      name: Path,
      contents: FileContents
  ) =
    map.update(m =>
      m.updated(name, contents) // Update cache
    )

  def currentValue(name: Path) =
    map.get.map(_.get(name))

object FileCache:
  val make =
    defer:
      FileCache(
        Ref
          .make[Map[Path, FileContents]](
            Map.empty
          )
          .run
      )

object FileService:
  val live =
    ZLayer.fromZIO(
      defer {
        val cache = FileCache.make.run
        val activeRefreshes =
          Ref
            .make[Map[Path, ActiveUpdate]](
              Map.empty
            )
            .run
        Live(
          Ref.make[Int](0).run,
          Ref.make[Int](0).run,
          cache,
          activeRefreshes,
          ZIO.service[FileSystem].run
        )
      }
    )

  case class ActiveUpdate(
      observers: Int,
      promise: Promise[Nothing, FileContents]
  ):
    def completeWith(contents: FileContents) =
      promise.succeed(contents)

  case class Live(
      hit: Ref[Int],
      miss: Ref[Int],
      // TODO Consider ConcurrentMap
      cache: FileCache,
      activeRefresh: Ref[
        Map[Path, ActiveUpdate]
      ],
      fileSystem: FileSystem
  ) extends FileService:

    def retrieveContents(
        name: Path
    ): ZIO[Any, Nothing, FileContents] =
      defer {
        cache.currentValue(name).run match
          case Some(initValue) =>
            hit.update(_ + 1).run
            printLine(
              "Value was cached. Easy path."
            ).orDie.run
            initValue
          case None =>
            retrieveOrWaitForContents(name).run
      }

    private def retrieveOrWaitForContents(
        name: Path
    ): ZIO[Any, Nothing, FileContents] =
      defer {
        val activeUpdates =
          calculateActiveUpdates(
            activeRefresh,
            name
          ).run
        val activeUpdate = activeUpdates(name)
        activeUpdate.observers match
          case 0 =>
            firstHerdMemberBehavior(
              fileSystem,
              activeUpdate,
              activeRefresh,
              miss,
              cache,
              name
            ).run
          case observerCount =>
            slowHerdMemberBehavior(
              hit,
              activeUpdate
            ).run
      }

    val hits: ZIO[Any, Nothing, Int] = hit.get

    val misses: ZIO[Any, Nothing, Int] = miss.get

  end Live
end FileService

def slowHerdMemberBehavior(
    hit: Ref[Int],
    activeUpdate: ActiveUpdate
) =
  defer:
    printLine(
      "Slower herd member will wait for response of 1st member"
    ).orDie.run
    hit.update(_ + 1).run
    activeUpdate
      .promise
      .await
      .tap(_ =>
        printLine(
          "Slower herd member got answer from 1st member"
        ).orDie
      )
      .run

def calculateActiveUpdates(
    activeRefresh: Ref[Map[Path, ActiveUpdate]],
    name: Path
) =
  defer:
    val promiseThatMightNotBeUsed =
      Promise.make[Nothing, FileContents].run
    activeRefresh
      .updateAndGet { activeRefreshes =>
        activeRefreshes.updatedWith(name) {
          case Some(activeUpdate) =>
            Some(
              activeUpdate.copy(observers =
                activeUpdate.observers + 1
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
      .run

def firstHerdMemberBehavior(
    fileSystem: FileSystem,
    activeUpdate: ActiveUpdate,
    activeRefresh: Ref[Map[Path, ActiveUpdate]],
    miss: Ref[Int],
    // TODO Consider ConcurrentMap
    cache: FileCache,
    name: Path
): ZIO[Any, Nothing, FileContents] =
  defer {
    printLine(
      "1st herd member will hit the filesystem"
    ).orDie.run
    val contents =
      fileSystem.readFileExpensive(name).run
    activeUpdate.completeWith(contents).run

    activeRefresh
      .update(m =>
        m - name // Clean out "active" entry
      )
      .run
    cache.saveContents(name, contents).run
    miss.update(_ + 1).run
    contents
  }

val users = (0 to 1000).toList.map("User " + _)
//  List("Bill", "Bruce", "James")

val herdBehavior =
  defer {
    val fileService =
      ZIO.service[FileService].run
    ZIO
      .foreachParDiscard(users)(user =>
        fileService.retrieveContents(
          Path.of("awesomeMemes")
        )
      )
      .run
    ZIO.debug("=========").run
    fileService
      .retrieveContents(Path.of("awesomeMemes"))
      .run
  }

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior
      .provide(FileSystem.live, FileService.live)

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

object FileSystem:
  val hardcodedFileContents =
    FileContents(
      List("viralImage1", "viralImage2")
    )
  val live = ZLayer.succeed(new FileSystem {})
