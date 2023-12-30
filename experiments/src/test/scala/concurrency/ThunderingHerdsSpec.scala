package concurrency

import zio.test.*

import java.nio.file.Path

object ThunderingHerdsSpec
    extends ZIOSpecDefault:
  val herdScenario =
    defer {
      val users = List("Bill", "Bruce", "James")

      val herdBehavior =
        defer {
          val popularService =
            ZIO.service[PopularService].run
          val fileResults =
            ZIO
              .foreachPar(users)(user =>
                popularService.retrieveContents(
                  Path.of("awesomeMemes")
                )
              )
              .run
          popularService
            .retrieveContents(
              Path.of("awesomeMemes")
            )
            .run
          fileResults
        }

      val logicFork = herdBehavior.fork.run
      TestClock.adjust(2.seconds).run
      val res: List[FileContents] = logicFork.join.run
      val misses: Int =
        ZIO
          .serviceWithZIO[PopularService](_.misses)
          .run
      (res, misses)

    }
  end herdScenario

  override def spec =
    suite("ThunderingHerdsSpec")(
      test(
        "classic happy path using zio-cache library"
      ) {
        defer:
          val (res, misses) = herdScenario.run
          assertTrue(
            misses == 1,
            res.forall(singleResult =>
              singleResult ==
                FSLive.hardcodedFileContents
            )
          )
      }.provide(
        FileSystem.live,
        ZLayer.fromZIO(
          ServiceThatCanHandleThunderingHerds
            .make
        )
      ),
      test(
        "sad path using no caching"
      ) {
        defer:
          val (res, misses) = herdScenario.run
          assertTrue(
            misses == 4,
            res.forall(singleResult =>
              singleResult ==
                FSLive.hardcodedFileContents
            )
          )
      }.provide(
        FileSystem.live,
        NoCacheAtAll.live
        )
    ) @@ TestAspect.withLiveClock
end ThunderingHerdsSpec
