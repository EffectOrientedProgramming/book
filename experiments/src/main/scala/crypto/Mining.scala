package crypto

import zio._
import ZIO.debug
import zio.Random.nextIntBetween

import java.io.IOException
import scala.annotation.tailrec

object Mining extends ZIOAppDefault:
  def run =
    for
      chain <- Ref.make[BlockChain](BlockChain())
      _     <- raceForNextBlock(chain).repeatN(5)
      _ <- chain.get.debug("Final")
    yield ()

  private val miners =
    Seq(
      "Zeb",
      "Frop",
      "Shtep"
    ).flatMap(minerName =>
      Range(1, 50)
        .map(i => new Miner(minerName + i))
    )

  def raceForNextBlock(
                        chain: Ref[BlockChain]
                      ): ZIO[Any, Nothing, Unit] =
    for
      raceResult <- findNextBlock(miners)
      (winner, winningPrime) = raceResult
      _ <-
        chain.update(chainCurrent =>
          chainCurrent.copy(blocks =
            chainCurrent.blocks :+ winningPrime
          )
        )
      _ <-
        debug(
          s"$winner mined block: $winningPrime"
        )
    yield ()

  case class BlockChain(
      blocks: List[Int] = List.empty
  )

  class Miner(val name: String):
    def mine(
        num: Int
    ): ZIO[Any, Nothing, (String, Int)] =
      for
        duration <- nextIntBetween(1, 4)
        _        <- ZIO.sleep(duration.second)
      yield (name, nextPrimeAfter(num))
  end Miner

  def findNextBlock(
      miners: Seq[Miner]
  ): ZIO[Any, Nothing, (String, Int)] =
    for
      startNum <-
        nextIntBetween(2000, 4000)
      result <-
        ZIO.raceAll(
          miners.head.mine(startNum),
          miners.tail.map(_.mine(startNum))
        )
    yield result

end Mining

// TODO Consider putting math functions somewhere else to avoid cluttering example

private def isPrime(num: Int): Boolean =
  (2 until num)
    .forall(divisor => num % divisor != 0)

@tailrec
private def nextPrimeAfter(num: Int): Int =
  if (isPrime(num))
    num
  else
    nextPrimeAfter(num + 1)

