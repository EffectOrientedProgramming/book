package concurrency

import zio.*
import zio.Console.printLine
import zio.test.*

import java.nio.file.Path

object ThunderingHerdsSpec
    extends ZIOSpecDefault:
  val testInnards =

    val users = List("Bill", "Bruce", "James")

    val herdBehavior =
      for
        fileService <- ZIO.service[FileService]
        fileResults <-
          ZIO.foreachPar(users)(user =>
            fileService.retrieveContents(
              Path.of("awesomeMemes")
            )
          )
        _ <- ZIO.debug("=========")
        _ <-
          fileService.retrieveContents(
            Path.of("awesomeMemes")
          )
      yield fileResults
    for
      _         <- printLine("Capture?")
      logicFork <- herdBehavior.fork
      _         <- TestClock.adjust(2.seconds)
      res       <- logicFork.join
      misses <-
        ZIO.serviceWithZIO[FileService](_.misses)
      _ <- ZIO.debug("Eh?")
    yield assertTrue(
      misses == 1,
      res.forall(singleResult =>
        singleResult ==
          FileSystem.hardcodedFileContents
      )
    )
  end testInnards

  override def spec =
    suite("ThunderingHerdsSpec")(
      test("classic happy path") {
        testInnards
      }.provide(
        FileSystem.live,
        FileService.live
      ),
      test(
        "classic happy path using zio-cache library"
      ) {
        testInnards
      }.provide(
        FileSystem.live,
        ZLayer.fromZIO(
          ThunderingHerdsUsingZioCacheLib.make
        )
      )
    )
end ThunderingHerdsSpec
