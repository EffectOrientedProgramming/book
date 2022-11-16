package test_aspects

import zio.*
import zio.test.*
import zio.test.TestAspect.*

object WithLiveSpec extends ZIOSpecDefault:

  def halfFlaky[A](a: A): ZIO[Any, String, A] =
    for
      b <- zio.Random.nextBoolean.debug
      o <-
        ZIO
          .cond(b, a, "failed")
          .tapError(ZIO.logError(_))
    yield o

  val song =
    for _ <- halfFlaky("works").debug
    yield assertCompletes

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
