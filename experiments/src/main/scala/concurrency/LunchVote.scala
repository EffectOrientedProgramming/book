package concurrency

import concurrency.LunchVote.Vote.Yay
import zio.*
import zio.concurrent.*

object LunchVote extends ZIOAppDefault:

  enum Vote:
    case Yay, Nay


  def getVoteFrom(person: String, results: ConcurrentMap[Vote, Int], votingComplete: Promise[Nothing, Vote], voterCount: Int): ZIO[Any, Nothing, Vote] =
    (for
      sleepAmount <- Random.nextIntBetween(1, 5)
      _ <- ZIO.sleep(sleepAmount.seconds)
      answer <- Random.nextBoolean.map(b => if(b) Vote.Yay else Vote.Nay).debug(s"$person vote")
      currentTally <- results.computeIfPresent(answer, (_, previous) => previous + 1).someOrFail(IllegalStateException("Vote not found")).orDie
      _ <- ZIO.when(currentTally > voterCount / 2)(votingComplete.succeed(answer))
//      _ <- votingComplete.isDone.debug(s"Voting complete when $person voted")
    yield answer).onInterrupt(ZIO.debug(s"interrupted $person vote"))

  def run =
    val voters = List(
      "Alice",
      "Bob",
      "Charlie",
      "Dave",
      "Eve",
    )
    for
      resultMap <- ConcurrentMap.make[Vote, Int](Vote.Yay -> 0, Vote.Nay -> 0)
      votingComplete <- Promise.make[Nothing, Vote]
      ongoingVotes <- ZIO.forkAll(
        voters.map(voter => getVoteFrom(voter, resultMap, votingComplete, voters.size)
        ))
      _ <- votingComplete.await.debug("Result")
//      _ <- votingComplete.isDone.debug("Done")
      _ <- ongoingVotes.interrupt
//      _ <- results.
    yield ()
