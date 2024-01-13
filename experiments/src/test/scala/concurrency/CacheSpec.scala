package concurrency

import zio.test.*

import java.nio.file.Path

object CacheSpec extends ZIOSpecDefault:
  val thunderingHerdsScenario =
    defer {
      // TODO Bigger list of random users.
      val users = List("Bill", "Bruce", "James")

      val herdBehavior =
        defer {
          val popularService =
            ZIO.service[PopularService].run
          val fileResults =
            ZIO
              .foreachPar(users)(user =>
                popularService.retrieve:
                  Path.of("awesomeMemes")
              )
              .run
          popularService
            .retrieve:
              Path.of("awesomeMemes")
            .run
          fileResults
        }

      val logicFork = herdBehavior.fork.run
      TestClock.adjust(2.seconds).run
      val res: List[FileContents] =
        logicFork.join.run
      val costToOperate: String =
        ZIO
          .serviceWithZIO[CloudStorage](
            _.invoice
          )
          .run
      (res, costToOperate)

    }
  end thunderingHerdsScenario

  override def spec =
    suite("CacheSpec")(
      test(
        "classic happy path using zio-cache library"
      ) {
        defer:
          val (res, costToOperate) =
            thunderingHerdsScenario.run
          assertTrue(
            costToOperate == "Amount owed: $1",
            res.forall(singleResult =>
              singleResult ==
                FSLive.hardcodedContents
            )
          )
      }.provide(
        CloudStorage.live,
        ZLayer.fromZIO(ServiceCached.make)
      ),
      test("sad path using no caching") {
        defer:
          val (res, costToOperate) =
            thunderingHerdsScenario.run
          assertTrue(
            costToOperate == "Amount owed: $4",
            res.forall(singleResult =>
              singleResult ==
                FSLive.hardcodedContents
            )
          )
      }.provide(
        CloudStorage.live,
        ServiceUncached.live
      )
    ) @@ TestAspect.withLiveClock
end CacheSpec
