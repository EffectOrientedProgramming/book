package concurrency

import concurrency.LunchVote.Vote.Yay
import zio.*
import zio.direct.*
import zio.concurrent.*

object LunchVote:

  enum Vote:
    case Yay,
      Nay

  case class Voter(
      name: String,
      delay: Duration,
      response: Vote,
      onInterrupt: ZIO[Any, Nothing, Unit] =
        ZIO.unit
  )

  def run(
      voters: List[Voter],
      maximumVoteTime: Duration =
        Duration.Infinity
  ) =
    defer {
      val resultMap =
        ConcurrentMap
          .make[Vote, Int](
            Vote.Yay -> 0,
            Vote.Nay -> 0
          )
          .run
      val voteProcesses =
        voters.map(voter =>
          getVoteFrom(
            voter,
            resultMap,
            voters.size
          ).onInterrupt(voter.onInterrupt)
        )
      ZIO
        .raceAll(
          voteProcesses.head,
          voteProcesses.tail
        )
        .timeout(maximumVoteTime)
        .some
        .run
    }
  end run

  case object NotConclusive

  def getVoteFrom(
      person: Voter,
      results: ConcurrentMap[Vote, Int],
      voterCount: Int
  ): ZIO[Any, NotConclusive.type, Vote] =
    defer {
      ZIO.sleep(person.delay).run
      val answer = person.response
      val currentTally =
        results
          .computeIfPresent(
            answer,
            (key, previous) => previous + 1
          )
          .someOrFail(
            IllegalStateException(
              "Vote not found"
            )
          )
          .orDie
          .run
      ZIO.when(currentTally <= voterCount / 2)(
        ZIO.fail(NotConclusive)
      ).run
      answer
    }

end LunchVote
