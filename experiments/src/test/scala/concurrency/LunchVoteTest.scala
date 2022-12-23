package concurrency

import zio.test._
import zio._
import LunchVote._
import LunchVote.Vote._

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
              Voter("Dave", 1.seconds, Nay, onInterrupt = interruptedVoters.updateAndGet(_ + 1).flatMap(cnt => ZIO.debug("Calling custom interrupt! cnt: " + cnt))),
              Voter("Eve", 1.seconds, Nay, onInterrupt = interruptedVoters.updateAndGet(_ + 1).flatMap(cnt => ZIO.debug("Calling custom interrupt! cnt: " + cnt)))
            )
          result <- LunchVote.run(voters)
          totalInterrupted <- interruptedVoters.get.debug("cnt")
//          _ <- ZIO.withClock(Clock.ClockLive)(ZIO.sleep(1.seconds))
        yield assertTrue(result == Yay) // TODO Figure out why interrupted count isn't working // && assertTrue(totalInterrupted == 2)
      },
      test("3 quick nays") {
        val voters =
          List(
            Voter("Alice", 0.seconds, Nay),
            Voter("Bob", 0.seconds, Nay),
            Voter("Charlie", 0.seconds, Nay),
            Voter("Dave", 1.seconds, Yay),
            Voter("Eve", 1.seconds, Yay)
          )
        for
          result <- LunchVote.run(voters)
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
          resultF <- LunchVote.run(voters, 1.seconds).fork
          _ <- TestClock.adjust(2.seconds)
          timeout <- resultF.join.flip
        yield assertTrue(timeout == None)
      }
    )
