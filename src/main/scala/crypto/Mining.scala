package crypto

import zio.console.{Console, getStrLn, putStrLn}
import zio.clock.currentTime
import zio.{Fiber, IO, Runtime, Schedule, UIO, ZIO, URIO, ZLayer, Ref}
import zio.duration._
import zio.random._

object Mining extends zio.App {

  def miner(name: String) =
    for
      duration <- nextInt.map(_.abs % 10 + 1)
      _ <- ZIO.sleep(duration.second)
    yield s"$name finished in $duration seconds"

  def run(args: List[String]) = //Use App's run function
    val logic =
      for
        raceResult <- miner("Zeb").race(miner("Frop"))
        _ <- putStrLn("Winner: " + raceResult)
      yield ()
    logic.exitCode

}
