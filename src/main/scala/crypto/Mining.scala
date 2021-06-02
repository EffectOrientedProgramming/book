package crypto

import zio.console.{Console, getStrLn, putStrLn}
import zio.clock.currentTime
import zio.{Fiber, IO, Runtime, Schedule, UIO, ZIO, URIO, ZLayer, Ref}
import zio.duration._
import zio.random._

object Mining extends zio.App {

  case class Miner(val name: String, gigaFlops: Int):
    val mineBySleeping =
      for
        duration <- nextIntBetween(1, 7)
        _ <- ZIO.sleep(duration.second)
      yield (this, s"$name mined the next coin in $duration seconds")

    //Takes a starting value, then calls itterates up through numbers until it finds a prime number.
    def mineByCalculatingPrimeNumbers(num: Long) =
      ZIO.succeed(
        (
          this,
          s"$name mined the next coin at prime number: ${findNextPrime(num / gigaFlops)}"
        )
      )

  // Nonempty list TODO embed in the type
  def findNextBlock(miners: Seq[Miner]) =
    miners
      .map(_.mineBySleeping)
//      .reduce { case (minersSoFar, nextMiner) => minersSoFar.race(nextMiner) }
      .reduce(_ race _) // Much terser. I think it's worth using this form

  def findNextBlock2(miners: Seq[Miner], startNum: Long) =
    miners
      .map(_.mineByCalculatingPrimeNumbers(startNum))
      .reduce(_.race(_))

  def run(args: List[String]) = //Use App's run function
    val frop = Miner("Wealthy Frop", gigaFlops = 120)
    val zeb = Miner("Average Zeb", gigaFlops = 100)
    val shtep = Miner("Poor Shtep", gigaFlops = 80)

    val calculateAndPrintWinner = //Uses mine1 function (Just sleeping)
      for
        startNum <- nextIntBetween(
          5_000_000,
          100_000_000
        ) //This is the value that the prime number finder starts from
        raceResult <- findNextBlock2(Seq(zeb, frop, shtep), startNum)
        (winner, winnerText) = raceResult
        _ <- putStrLn(winnerText)
      yield (winner)

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
          finalCoinResults.toList
            .sortBy(_._2)
            .reverse
            .map { x =>
              x.toString
            }
            .mkString("\n")
        )
      yield ()

    val repetitions = 50
    // Demonstrating Refs vs Fold. These 2 approaches are the exact same
    val miningWithResults =
      miningAttempt(
        for
          coinResults <- Ref.make(Map(frop -> 0, zeb -> 0, shtep -> 0))
          _ <-
            (for
              winner <-
                calculateAndPrintWinner // TODO Factor out since it exists in the other approach too?
              currentCoins: Map[Miner, Int] <- coinResults.get
              _ <- coinAccounts.set(
                currentCoins.updated(winner, (currentCoins(winner) + 1))
              )
            yield ()).repeatN(repetitions)
          finalCoinResults: Map[Miner, Int] <- coinResults.get
        yield finalCoinResults
      )

    val miningWithResultsFold =
      miningAttempt(
        ZIO.foldLeft((1 to repetitions))(Map(frop -> 0, zeb -> 0, shtep -> 0)) {
          case (coinResults, _) =>
            for
              winner <-
                calculateAndPrintWinner // TODO Factor out since it exists in the other approach too?
            yield coinResults.updated(winner, (coinResults(winner) + 1))
        }
      )

//    logic1
//      .repeatN(5)
//      .exitCode

    miningWithResults.exitCode
//    miningWithResultsFold.exitCode

//    logic2
//      .repeatN(5)
//      .exitCode

  //Inefficiently determines if the input number is prime.
  def isPrime(num: Long): Boolean =
    (2 to num.toInt - 1).forall(num % _ != 0)

  //Recursivley itterates up from starting value, num, until it finds a prime number, which it returns
  def findNextPrime(num: Long): Long =
    if (isPrime(num))
      num
    else
      findNextPrime(num + 1)
}
