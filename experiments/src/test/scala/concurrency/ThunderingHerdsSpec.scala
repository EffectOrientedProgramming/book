package concurrency

import zio.*
import zio.test.*

import java.nio.file.Path

object ThunderingHerdsSpec extends ZIOSpecDefault:
  override def spec =
    suite("ThunderingHerdsSpec")(
      test("classic happy path"){

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
        for
          logicFork <- herdBehavior.fork
          _ <- TestClock.adjust(2.seconds)
          res <- logicFork.join
          misses <- ZIO.serviceWithZIO[FileService](_.misses)
          _ <- ZIO.debug("Eh?")
        yield assertTrue(misses == 3)
      }.provide(
        FileSystem.live,
        FileService.live,
      )
    )
