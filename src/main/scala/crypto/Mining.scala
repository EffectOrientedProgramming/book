package crypto

import zio.console.{Console, getStrLn, putStrLn}
import zio.clock.currentTime
import zio.{Fiber, IO, Runtime, Schedule, UIO, ZIO, URIO, ZLayer, Ref}
import zio.duration._
import zio.random._

object Mining extends zio.App {

  case class Miner(val name: String):
    val mine =
      for
        duration <- nextIntBetween(1, 7)
        _ <- ZIO.sleep(duration.second)
      yield (this, s"$name mined the next coin in $duration seconds")

    //Inefficiently determines if the input number is prime.
    def isPrime(num: Long): Boolean =
      (2 to num.toInt - 1).forall(num % _ != 0)

    //Recursivley itterates up from starting value, num, until it finds a prime number, which it returns
    def findNextPrime(num: Long): Long =
      if (isPrime(num))
        num
      else
        findNextPrime(num + 1)

    //Takes a starting value, then calls itterates up through numbers until it finds a prime number.
    def mine2(num: Long) =
      ZIO.succeed(
        (
          this,
          s"$name mined the next coin at prime number: ${findNextPrime(num)}"
        )
      )

  // Nonempty list TODO embed in the type
  def findNextBlock(miners: Seq[Miner]) =
    miners
      .map(_.mine)
//      .reduce { case (minersSoFar, nextMiner) => minersSoFar.race(nextMiner) }
      .reduce(_ race _) // Much terser. I think it's worth using this form

  def findNextBlock2(miners: Seq[Miner], startNum: Long) =
    miners
      .map(_.mine2(startNum))
      .reduce(_.race(_))

  def run(args: List[String]) = //Use App's run function
    val zeb = Miner("Zeb")
    val frop = Miner("Frop")
    val shtep = Miner("Shtep")

    val logic1 = //Uses mine1 function (Just sleeping)
      for
        startNum <- nextIntBetween(
          5_000_000,
          100_000_000
        ) //This is the value that the prime number finder starts from
        raceResult <- findNextBlock2(Seq(zeb, frop, shtep), startNum)
        (winner, winnerText) = raceResult
        _ <- putStrLn("Winner: " + winnerText)
      yield (winner)

    def recordWinner(coinAccounts: Ref[Map[Miner, Int]]) =
      for
        winner <- logic1
        currentCoins: Map[Miner, Int] <- coinAccounts.get
        _ <- coinAccounts.set(
          currentCoins.updated(winner, (currentCoins(winner) + 1))
        )
      yield ()

    def recordWinnerFold(coinAccounts: Map[Miner, Int]) =
      for winner <- logic1
      yield coinAccounts.updated(winner, (coinAccounts(winner) + 1))

    val logic2 = //Uses mine2 function (sleep and find prime numbers)
      for
        startNum <- nextInt.map(
          _.abs % 1000000 + 1000000
        ) //This is the value that the prime number finder starts from
        raceResult <- findNextBlock2(Seq(zeb, frop, shtep), startNum)
        _ <- putStrLn("Winner: " + raceResult)
      yield ()

    def miningAttempt(
        innards: ZIO[zio.random.Random & zio.Has[
          zio.clock.Clock.Service
        ] & zio.console.Console, java.io.IOException, Map[Miner, Int]]
    ) =
      for
        finalCoinResults: Map[Miner, Int] <- innards
        _ <- putStrLn(
          finalCoinResults
            .map { x =>
              x.toString
            }
            .mkString("\n")
        )
      yield ()

    // Demonstrating Refs vs Fold. These 2 approaches are the exact same
    val miningWithResults =
      miningAttempt(
        for
          coinResults <- Ref.make(Map(frop -> 0, zeb -> 0, shtep -> 0))
          _ <- recordWinner(coinResults).repeatN(5)
          finalCoinResults: Map[Miner, Int] <- coinResults.get
        yield finalCoinResults
      )

    val miningWithResultsFold =
      miningAttempt(
        ZIO.foldLeft((1 to 5))(Map(frop -> 0, zeb -> 0, shtep -> 0)) {
          case (coinResults, _) => recordWinnerFold(coinResults)
        }
      )

//    logic1
//      .repeatN(5)
//      .exitCode

//    miningWithResults.exitCode

    miningWithResultsFold.exitCode
//    logic2
//      .repeatN(5)
//      .exitCode

}
