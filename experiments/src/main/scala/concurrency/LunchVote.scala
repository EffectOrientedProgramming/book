package concurrency

import concurrency.LunchVote.Vote.Yay
import zio.*
import zio.concurrent.*

object LunchVote extends ZIOAppDefault:

  enum Vote:
    case Yay,
      Nay

  def run =
    val voters =
      List(
        "Alice",
        "Bob",
        "Charlie",
        "Dave",
        "Eve"
      )
    for
      resultMap <-
        ConcurrentMap.make[Vote, Int](
          Vote.Yay -> 0,
          Vote.Nay -> 0
        )
      voteProcesses = voters.map(voter =>
            getVoteFrom(
              voter,
              resultMap,
              voters.size
            ).onInterrupt(
              ZIO.debug(s"interrupted $voter")
            )
          )
      result <-
        ZIO.raceAll(
          voteProcesses.head,
          voteProcesses.tail,
        )
      _ <- ZIO.debug("Result: " + result)
    yield ()
    end for
  end run

  case object NotConclusive

  def getVoteFrom(
                   person: String,
                   results: ConcurrentMap[Vote, Int],
                   voterCount: Int
                 ): ZIO[Any, NotConclusive.type, Vote] =
    for
      sleepAmount <-
        Random.nextIntBetween(1, 5)
      _ <- ZIO.sleep(sleepAmount.seconds)
      answer <-
        Random
          .nextBoolean
          .map(b =>
            if (b)
              Vote.Yay
            else
              Vote.Nay
          )
          .debug(s"$person vote")
      currentTally <-
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
      _ <-
        ZIO.when(
          currentTally <= voterCount / 2
        )(ZIO.fail(NotConclusive))
    yield answer

end LunchVote
