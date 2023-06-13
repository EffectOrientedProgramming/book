package test_aspects

import zio.*
import zio.direct.*
import zio.test.*
import zio.test.TestAspect.*

object WithLiveSpec extends ZIOSpecDefault:

  def halfFlaky[A](a: A): ZIO[Any, String, A] =
    defer {
      val b = zio.Random.nextBoolean.debug.run
      ZIO
        .cond(b, a, "failed")
        .tapError(ZIO.logError(_)).run
    }

  val song =
    defer {
      halfFlaky("works").debug.run
      assertCompletes
    }

  val song1: Spec[Any, String] =
    test("Song 1")(song)

  val songFlaky
      : Spec[Live & Annotations, String] =
    test("Song Flaky")(song) @@ flaky(10) @@
      withLiveRandom

  val spec =
    suite("Play some music")(
      song1,
      songFlaky,
      test("Song 2")(assertCompletes)
    )
end WithLiveSpec
