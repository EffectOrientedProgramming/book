package crypto

import zio.console.{Console, getStrLn, putStrLn}
import zio.clock.currentTime
import zio.{Fiber, IO, Runtime, Schedule, UIO, ZIO, URIO, ZLayer, Ref}
import zio.duration._
import zio.random._

object Mining extends zio.App {

  class Miner(val name: String):
    val mine =
      for
        duration <- nextInt.map(_.abs % 7 + 1)
        _ <- ZIO.sleep(duration.second)
      yield s"$name mined the next coin in $duration seconds"

  // Nonempty list TODO embed in the type
  def findNextBlock(miners: Seq[Miner]) =
    miners
      .map(_.mine)
      .reduce { case (minersSoFar, nextMiner) => minersSoFar.race(nextMiner) }

  def run(args: List[String]) = //Use App's run function
    val zeb = Miner("Zeb")
    val frop = Miner("Frop")
    val shtep = Miner("Shtep")
    val logic =
      for
        raceResult <- findNextBlock(Seq(zeb, frop, shtep))
        _ <- putStrLn("Winner: " + raceResult)
      yield ()
    logic
      .repeatN(5)
      .exitCode

}
