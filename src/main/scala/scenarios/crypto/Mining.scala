package scenarios.crypto

import zio.Console.{printLine, readLine}
import zio.{
  Clock,
  Console,
  Fiber,
  Has,
  IO,
  Random,
  Ref,
  Runtime,
  Schedule,
  UIO,
  URIO,
  ZIO,
  ZLayer,
  durationInt
}
import zio.Clock.currentTime
import zio.Duration.*
import zio.Random.*

import java.io.IOException

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
    def mine2(num: Long): ZIO[Has[
      zio.Random
    ] & Has[Clock], Nothing, Object] =
      for
        duration <- nextIntBetween(1, 4)
        _ <- ZIO.sleep(duration.second)
        prime = findNextPrime(num)
      yield s"$name mined the next coin at prime number: $prime"
  end Miner

  // Nonempty list TODO embed in the type
  def findNextBlock(miners: Seq[Miner]) =
    miners
      .map(_.mine)
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
        _ <- printLine("Winner: " + raceResult)
      yield ()

    val logic2: ZIO[Has[Console] with Has[
      Random
    ] with Has[Clock], IOException, Unit] = //Uses mine2 function (sleep
      // and find
      // prime numbers)
      for
        startNum <-
          nextIntBetween(1000000, 2000000)
        raceResult <-
          findNextBlock2(
            Seq(zeb, frop, shtep),
            startNum
          )
        _ <- printLine("Winner: " + raceResult)
      yield ()

    logic2.repeatN(5).exitCode
  end run
end Mining
