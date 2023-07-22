package concurrency

import zio.Console.printLine
import zio.test.*

import java.nio.file.Path

object ThunderingHerdsSpec
    extends ZIOSpecDefault:
  val testInnards =
    defer {
      val users = List("Bill", "Bruce", "James")

      val herdBehavior =
        defer {
          val fileService =
            ZIO.service[FileService].run
          val fileResults =
            ZIO
              .foreachPar(users)(user =>
                fileService.retrieveContents(
                  Path.of("awesomeMemes")
                )
              )
              .run
          ZIO.debug("=========").run
          fileService
            .retrieveContents(
              Path.of("awesomeMemes")
            )
            .run
          fileResults
        }

      printLine("Capture?").run
      val logicFork = herdBehavior.fork.run
      TestClock.adjust(2.seconds).run
      val res = logicFork.join.run
      val misses =
        ZIO
          .serviceWithZIO[FileService](_.misses)
          .run
      ZIO.debug("Eh?").run

      assertTrue(
        misses == 1,
        res.forall(singleResult =>
          singleResult ==
            FileSystem.hardcodedFileContents
        )
      )
    }
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
