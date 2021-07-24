package scenarios.crypto

import zio.console.{getStrLn, putStrLn, Console}
import zio.clock.currentTime
import zio.{
  Fiber,
  IO,
  Ref,
  Runtime,
  Schedule,
  UIO,
  URIO,
  ZIO,
  ZLayer
}
import zio.duration._
import zio.random._

object Mining extends zio.App:

  class Miner(val name: String):

    val mine =
      for
        duration <- nextInt.map(_.abs % 7 + 1)
        _ <- ZIO.sleep(duration.second)
      yield s"$name mined the next coin in $duration seconds"

    // Inefficiently determines if the input
    // number is prime.
    def isPrime(num: Long): Boolean =
      for (i <- 2 to num.toInt - 1)
        if (num % i == 0)
          return false
      return true

    // Recursivley itterates up from starting
    // value, num, until it finds a prime number,
    // which it returns
    def findNextPrime(num: Long): Long =
      if (isPrime(num))
        num
      else
        findNextPrime(num + 1)

    // Takes a starting value, then calls
    // itterates up through numbers until it
    // finds a prime number.
    def mine2(num: Long): ZIO[
      zio.random.Random &
        zio.Has[zio.clock.Clock.Service],
      Nothing,
      Object
    ] =
      for
        duration <- nextInt.map(_.abs % 3 + 1)
        _ <- ZIO.sleep(duration.second)
        prime = findNextPrime(num)
      yield s"$name mined the next coin at prime number: $prime"
  end Miner

  // Nonempty list TODO embed in the type
  def findNextBlock(miners: Seq[Miner]) =
    miners
      .map(_.mine)
// .reduce { case (minersSoFar, nextMiner)
      // => minersSoFar.race(nextMiner) }
      .reduce(
        _.race(_)
      ) // Much terser. I think it's worth using this form

  def findNextBlock2(
      miners: Seq[Miner],
      startNum: Long
  ) =
    miners
      .map(_.mine2(startNum))
      .reduce(_.race(_))

  def run(
      args: List[String]
  ) = //Use App's run function
    val zeb = Miner("Zeb")
    val frop = Miner("Frop")
    val shtep = Miner("Shtep")
    val logic1 = //Uses mine1 function (Just sleeping)
      for
        raceResult <-
          findNextBlock(Seq(zeb, frop, shtep))
        _ <- putStrLn("Winner: " + raceResult)
      yield ()

    val logic2 = //Uses mine2 function (sleep and find prime numbers)
      for
        startNum <-
          nextInt.map(
            _.abs % 1000000 + 1000000
          ) //This is the value that the prime number finder starts from
        raceResult <-
          findNextBlock2(
            Seq(zeb, frop, shtep),
            startNum
          )
        _ <- putStrLn("Winner: " + raceResult)
      yield ()
    /* logic1 .repeatN(5) .exitCode */

    logic2.repeatN(5).exitCode
  end run
end Mining
