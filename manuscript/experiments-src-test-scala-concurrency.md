## experiments-src-test-scala-concurrency

 

### experiments/src/test/scala/concurrency/LunchVoteTest.scala
```scala
package concurrency

import zio.direct.*
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
        defer {
          val interruptedVoters = Ref.make(0).run
          val voters =
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
          val result = LunchVote.run(voters).run
          val totalInterrupted =
            interruptedVoters.get.run
          //          _ <- ZIO.withClock(Clock.ClockLive)(ZIO.sleep(1.seconds))
          assertTrue(result == Yay) &&
            assertTrue(totalInterrupted == 2)
        }
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
        defer {
          val result = LunchVote.run(voters).run
          assertTrue(result == Nay)
        }
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
        defer {
          val resultF =
            LunchVote
              .run(
                voters,
                maximumVoteTime = 1.seconds
              )
              .fork
              .run
          TestClock.adjust(2.seconds).run
          val timeout = resultF.join.flip.run
          assertTrue(timeout.isEmpty)
        }
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
import zio.direct.*

import java.nio.file.Path

object ThunderingHerdsSpec
    extends ZIOSpecDefault:
  val testInnards =
    defer {
      val users = List("Bill", "Bruce", "James")

      val herdBehavior =
        defer {
          val fileService = ZIO.service[FileService].run
          val fileResults =
            ZIO.foreachPar(users)(user =>
              fileService.retrieveContents(
                Path.of("awesomeMemes")
              )
            ).run
          ZIO.debug("=========").run
          fileService.retrieveContents(
            Path.of("awesomeMemes")
          ).run
          fileResults
        }

      printLine("Capture?").run
      val logicFork = herdBehavior.fork.run
      TestClock.adjust(2.seconds).run
      val res       = logicFork.join.run
      val misses =
        ZIO.serviceWithZIO[FileService](_.misses).run
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
      ),
    )
end ThunderingHerdsSpec

```


