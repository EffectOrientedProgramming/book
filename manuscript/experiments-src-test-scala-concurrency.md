## experiments-src-test-scala-concurrency

 

### experiments/src/test/scala/concurrency/LunchVoteTest.scala
```scala
package concurrency

import zio.test.*
import zio.test.TestAspect.*
import zio.test.Assertion.*
import zio.*
import LunchVote.*
import LunchVote.Vote.*

object LunchVoteTest extends ZIOSpecDefault:
  def spec =
    suite("voting situations")(
      test("3 quick yays") {
        for
          interruptedVoters <- Ref.make(0)
          voters =
            List(
              Voter("Alice", 0.seconds, Yay),
              Voter("Bob", 0.seconds, Yay),
              Voter("Charlie", 0.seconds, Yay),
              Voter(
                "Dave",
                1.seconds,
                Nay,
                onInterrupt =
                  interruptedVoters.update(_ + 1)
              ),
              Voter(
                "Eve",
                1.seconds,
                Nay,
                onInterrupt =
                  interruptedVoters.update(_ + 1)
              )
            )
          result <- LunchVote.run(voters)
          totalInterrupted <-
            interruptedVoters.get
//          _ <- ZIO.withClock(Clock.ClockLive)(ZIO.sleep(1.seconds))
        yield assertTrue(result == Yay) &&
          assertTrue(totalInterrupted == 2)
      } @@
        flaky, // Flaky because Interruption count is not reliable
      test("3 quick nays") {
        val voters =
          List(
            Voter("Alice", 0.seconds, Nay),
            Voter("Bob", 0.seconds, Nay),
            Voter("Charlie", 0.seconds, Nay),
            Voter("Dave", 1.seconds, Yay),
            Voter("Eve", 1.seconds, Yay)
          )
        for result <- LunchVote.run(voters)
        yield assertTrue(result == Nay)
      },
      test("slow voters") {
        val voters =
          List(
            Voter("Alice", 10.seconds, Nay),
            Voter("Bob", 10.seconds, Nay),
            Voter("Charlie", 10.seconds, Nay),
            Voter("Dave", 10.seconds, Yay),
            Voter("Eve", 10.seconds, Yay)
          )
        for
          resultF <-
            LunchVote
              .run(
                voters,
                maximumVoteTime = 1.seconds
              )
              .fork
          _       <- TestClock.adjust(2.seconds)
          timeout <- resultF.join.flip
        // yield assert(timeout)(isNone)
        yield assertTrue(timeout.isEmpty)
      }
    )
end LunchVoteTest

```


### experiments/src/test/scala/concurrency/ThunderingHerdsSpec.scala
```scala
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
    yield assertTrue(misses == 1) &&
      assertTrue(
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
      ),
    )
end ThunderingHerdsSpec

```


