package crypto

import zio.console.{Console, getStrLn, putStrLn}
import zio.clock.currentTime
import zio.{Fiber, IO, Runtime, Schedule, UIO, ZIO, URIO, ZLayer, Ref}
import zio.duration._
import zio.random._

object Mining extends zio.App {

  case class Miner(val name: String, gigaFlops: Int):
    def mineBySleeping(maxSleepSeconds: Int) =
      for
        duration <- nextIntBetween(1, maxSleepSeconds)
        _ <- ZIO.sleep(duration.second)
      yield (this, s"$name mined the next coin in $duration seconds")

    //Takes a starting value, then calls itterates up through numbers until it finds a prime number.
    def mineByCalculatingPrimeNumbers(num: Long): ZIO[Any, Nothing, (Miner, String)] =
      ZIO.succeed(
        (
          this,
          s"$name mined the next coin at prime number: ${findNextPrime(num / gigaFlops)}"
        )
      )


  // Nonempty list TODO embed in the type
  def competeForNextBlock(miners: Seq[Miner], f: Miner => ZIO[zio.random.Random & zio.Has[zio.clock.Clock.Service] & zio.Has[zio.console.Console.Service],
    Exception, (Miner, String)]) =
    miners
      .map(f(_))
      //      .foldLeft(()) // TODO Consider a cheating opportunity
      .reduce(_ race _) // Much terser. I think it's worth using this form

  // Nonempty list TODO embed in the type
  def competeForNextBlockPar(miners: Seq[Miner], f: Miner => ZIO[zio.random.Random & zio.Has[zio.clock.Clock.Service] &
    zio.Has[zio.console.Console.Service],
    Exception, (Miner, String)]) =
    val (head :: tail) = miners.map(miner => f(miner))
    val res =
      ZIO.reduceAllPar(
        head,
        tail
      )((first, second) => first)
    res
//      .foldLeft(()) // TODO Consider a cheating opportunity
//      .reduce(_ race _) // Much terser. I think it's worth using this form

  def findNextBlock(miners: Seq[Miner]) =
    competeForNextBlock(miners, _.mineBySleeping(10))

  def findNextBlock2(miners: Seq[Miner]) =
    for
      startNum <- nextIntBetween(
        5_000_000,
        100_000_000
      ) //This is the value that the prime number finder starts from
      (miner, completionMessage) <- competeForNextBlockPar(miners, _.mineByCalculatingPrimeNumbers(startNum))
    yield ((miner, completionMessage))

  def run(args: List[String]) = //Use App's run function
    val frop = Miner("Wealthy Frop", gigaFlops = 120)
    val zeb = Miner("Average Zeb", gigaFlops = 100)
    val shtep = Miner("Poor Shtep", gigaFlops = 80)

    val cheep = Miner("Cheatin' cheep", gigaFlops = 150)

    val miners = Seq(cheep, zeb, frop, shtep)

    val calculateAndPrintWinner = //Uses mine1 function (Just sleeping)
      for
        raceResult <- findNextBlock2(miners)
        (winner, winnerText) = raceResult
        _ <- putStrLn(winnerText)
      yield (winner)

    def miningAttempt(
        innards: ZIO[zio.random.Random & zio.Has[
          zio.clock.Clock.Service
        ] & zio.console.Console, Exception, Map[Miner, Int]]
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
          coinResults <- Ref.make(Map(miners.map(_ -> 0):_*))
          _ <-
            (for
              // TODO Factor out since it exists in the other approach too?
              winner <- calculateAndPrintWinner
              currentCoins: Map[Miner, Int] <- coinResults.get
              _ <- coinResults.set(
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
              // TODO Factor out since it exists in the other approach too?
              winner <- calculateAndPrintWinner
            yield coinResults.updated(winner, (coinResults(winner) + 1))
        }
      )

    miningWithResults.exitCode

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
