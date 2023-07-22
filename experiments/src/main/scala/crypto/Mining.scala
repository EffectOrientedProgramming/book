package crypto

import ZIO.debug
import zio.Random.nextIntBetween

import java.io.IOException
import scala.annotation.tailrec

object Mining extends ZIOAppDefault:
  def run =
    defer {
      val chain =
        Ref.make[BlockChain](BlockChain()).run
      raceForNextBlock(chain).repeatN(5).run
      chain.get.debug("Final").run
    }

  private val miners =
    Seq("Zeb", "Frop", "Shtep")
      .flatMap(minerName =>
        Range(1, 50)
          .map(i => new Miner(minerName + i))
      )

  def raceForNextBlock(
      chain: Ref[BlockChain]
  ): ZIO[Any, Nothing, Unit] =
    defer {
      val raceResult = findNextBlock(miners).run
      val (winner, winningPrime) = raceResult
      chain
        .update(chainCurrent =>
          chainCurrent.copy(blocks =
            chainCurrent.blocks :+ winningPrime
          )
        )
        .run
      debug(
        s"$winner mined block: $winningPrime"
      ).run
    }

  case class BlockChain(
      blocks: List[Int] = List.empty
  )

  class Miner(val name: String):
    def mine(
        num: Int
    ): ZIO[Any, Nothing, (String, Int)] =
      defer {
        val duration = nextIntBetween(1, 4).run
        ZIO.sleep(duration.second).run
        (name, nextPrimeAfter(num))
      }

  def findNextBlock(
      miners: Seq[Miner]
  ): ZIO[Any, Nothing, (String, Int)] =
    defer {
      val startNum =
        nextIntBetween(80000000, 160000000).run

      ZIO
        .raceAll(
          miners.head.mine(startNum),
          miners.tail.map(_.mine(startNum))
        )
        .run
    }

end Mining

// TODO Consider putting math functions somewhere else to avoid cluttering example

def isPrime(num: Int): Boolean = (2 until num)
  .forall(divisor => num % divisor != 0)

@tailrec
def nextPrimeAfter(num: Int): Int =
  if (isPrime(num))
    num
  else
    nextPrimeAfter(num + 1)
