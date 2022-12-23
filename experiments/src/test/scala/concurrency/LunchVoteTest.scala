package concurrency

import zio.test._
import zio._
import LunchVote._
import LunchVote.Vote._

object LunchVoteTest extends ZIOSpecDefault:
  def spec =
    suite("voting situations")(
      test("3 quick yays") {
        val voters =
          List(
            Voter("Alice", 0.seconds, Yay),
            Voter("Bob", 0.seconds, Yay),
            Voter("Charlie", 0.seconds, Yay),
            Voter("Dave", 1.seconds, Nay),
            Voter("Eve", 1.seconds, Nay)
          )
        for
          result <- LunchVote.run(voters)
        yield assertTrue(result == Yay)
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
      }
    )
