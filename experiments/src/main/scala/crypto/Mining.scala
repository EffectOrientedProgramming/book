package crypto

import zio.Console.{printLine, readLine}
import zio.{
  Clock,
  Console,
  Fiber,
  IO,
  Random,
  Ref,
  Runtime,
  Schedule,
  UIO,
  URIO,
  ZIO,
  ZIOAppDefault,
  ZLayer,
  durationInt
}
import zio.Clock.currentTime
import zio.Duration.*
import zio.Random.*

import java.io.IOException

object Mining extends ZIOAppDefault:

  class Miner(val name: String):

    val mine =
      for
        duration <- nextInt.map(_.abs % 7 + 1)
        _        <- ZIO.sleep(duration.second)
      yield s"$name mined the next coin in $duration seconds"

    // Inefficiently determines if the input
    // number is prime.
    def isPrime(num: Int): Boolean =
      for (i <- 2 to num.toInt - 1)
        if (num % i == 0)
          return false
      return true

    // Recursively iterates up from starting
    // value, num, until it finds a prime number,
    // which it returns
    def findNextPrime(num: Int): Int =
      if (isPrime(num))
        num
      else
        findNextPrime(num + 1)

    // Takes a starting value, then calls
    // iterates up through numbers until it
    // finds a prime number.
    def mine2(
        num: Int
    ): ZIO[Any, Nothing, (String, Int)] =
      for
        duration <- nextIntBetween(1, 4)
        _        <- ZIO.sleep(duration.second)
        prime = findNextPrime(num)
      yield (name, prime)
  end Miner

  def findNextBlock(
      miners: Seq[Miner],
      startNum: Int
  ): ZIO[Any, Nothing, (String, Int)] =
    ZIO.raceAll(
      miners.head.mine2(startNum),
      miners.tail.map(_.mine2(startNum))
    )

  def run = // Use App's run function
    val zeb   = Miner("Zeb")
    val frop  = Miner("Frop")
    val shtep = Miner("Shtep")

    val miners =
      Seq(zeb, frop, shtep).flatMap(miner =>
        Range(1, 50)
          .map(i => new Miner(miner.name + i))
      )

    def loopLogic(chain: Ref[List[Int]]): ZIO[
      Any,
      IOException,
      Unit
    ] = // Uses mine2 function (sleep
      // and find
      // prime numbers)
      for
        startNum <-
          nextIntBetween(20000000, 40000000)
        raceResult <-
          findNextBlock(miners, startNum)
        (winner, winningPrime) = raceResult
        _ <- chain.update(_ :+ winningPrime)
        _ <-
          printLine(
            s"$winner mined the next coin at prime number: $winningPrime"
          )
      yield ()

    val fullLogic =
      for
        chain <- Ref.make[List[Int]](List.empty)
        _     <- loopLogic(chain).repeatN(5)
        finalChain <- chain.get
        _ <-
          printLine("Final Chain: " + finalChain)
      yield ()

    fullLogic.exitCode
  end run
end Mining
