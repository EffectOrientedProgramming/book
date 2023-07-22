package Parallelism

import java.io.IOException

import scala.concurrent.Await

object ParallelSleepers extends ZIOAppDefault:

  override def run =
    defer(Use.withParallelEval) {
      for _ <- 1 to 10_000 do
        ZIO.sleep(1.seconds).run

      ZIO
        .debug(
          "Finished far sooner than 10,000 seconds"
        )
        .run
    }

val sleepers =
  Seq(
    1.seconds,
    2.seconds,
    3.seconds,
    4.seconds,
    5.seconds
  )

object ParallelSleepers2 extends ZIOAppDefault:
  override def run =
    ZIO
      .foreach(sleepers)(ZIO.sleep(_))
      .timed
      .debug

object ParallelSleepers3 extends ZIOAppDefault:
  override def run =
    ZIO
      .foreachPar(sleepers)(ZIO.sleep(_))
      .timed
      .debug

object ParallelSleepers4 extends ZIOAppDefault:
  override def run =
    val racers = sleepers.map(ZIO.sleep(_))
    ZIO
      .raceAll(racers.head, racers.tail)
      .timed
      .debug

object ParallelSleepers5 extends ZIOAppDefault:
  override def run =
    ZIO.withParallelism(2) {
      ZIO
        .foreachPar(sleepers)(ZIO.sleep(_))
        .timed
        .debug
    }
