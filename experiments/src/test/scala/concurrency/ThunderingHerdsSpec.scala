package concurrency

import zio.*
import zio.test.*

import java.nio.file.Path

object ThunderingHerdsSpec
    extends ZIOSpecDefault:
  val testInnards = {

    val users =
      List("Bill", "Bruce", "James")

    val herdBehavior =
      for
        fileService <-
          ZIO.service[FileService]
        fileResults <-
          ZIO
            .foreachPar(users)(user =>
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
      logicFork <- herdBehavior.fork
      _   <- TestClock.adjust(2.seconds)
      res <- logicFork.join
      misses <-
        ZIO.serviceWithZIO[FileService](
          _.misses
        )
      _ <- ZIO.debug("Eh?")
    yield assertTrue(misses == 2) && assertTrue(res.forall(singleResult => singleResult ==  FileSystem.hardcodedFileContents ))
  }

  override def spec =
    suite("ThunderingHerdsSpec")(
      test("classic happy path") {
        testInnards
      }.provide(
        FileSystem.live,
        FileService.live
      ),

      test("classic happy path using zio-cache library") {
        testInnards
      }.provide(
        FileSystem.live,
        ZLayer.fromZIO(ThunderingHerdsUsingZioCacheLib.make)
      )
    )
end ThunderingHerdsSpec
