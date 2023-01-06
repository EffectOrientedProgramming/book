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
            LunchVote.run(voters, maximumVoteTime = 1.seconds).fork
          _       <- TestClock.adjust(2.seconds)
          timeout <- resultF.join.flip
        //yield assert(timeout)(isNone)
        yield assertTrue(timeout.isEmpty)
      }
    )
end LunchVoteTest
