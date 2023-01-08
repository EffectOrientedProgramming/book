package Parallelism

import java.io.IOException
import zio.{
  Fiber,
  IO,
  Runtime,
  UIO,
  Unsafe,
  ZIO,
  ZIOAppDefault,
  ZLayer,
  durationInt
}

import scala.concurrent.Await

object ParallelSleepers extends ZIOAppDefault:

  override def run =
    ZIO.foreachPar(1 to 10_000)(_ =>
      ZIO.sleep(1.seconds)
    ) *>
      ZIO.debug(
        "Finished far sooner than 10,000 seconds"
      )

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
