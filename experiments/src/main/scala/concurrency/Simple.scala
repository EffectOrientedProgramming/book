package concurrency

import zio.{durationInt, duration2DurationOps, Clock, Console, Duration, ZIO, ZIOAppDefault}

def sleepThenPrint(d: Duration): ZIO[Clock & Console, java.io.IOException, Duration] =
  for
    _ <- ZIO.sleep(d)
    _ <- Console.printLine(s"${d.render} elapsed")
  yield d

object ForkDemo extends zio.ZIOAppDefault:
  override def run = for
    f1 <- sleepThenPrint(2.seconds).fork
    f2 <- sleepThenPrint(1.seconds).fork
    _  <- f1.join
    _  <- f2.join
  yield ()

object ForEachDemo extends zio.ZIOAppDefault:
  override def run =
    ZIO.foreach(Seq(2, 1)) { i =>
      sleepThenPrint(i.seconds)
    }

object ForEachParDemo extends zio.ZIOAppDefault:
  override def run =
    ZIO.foreachPar(Seq(2, 1)) { i =>
      sleepThenPrint(i.seconds)
    }

object RaceDemo extends zio.ZIOAppDefault:
  override def run =
    ZIO.raceAll(sleepThenPrint(2.seconds),
      Seq(sleepThenPrint(1.seconds))
    )
    /*
    // alternate syntax:
    sleepThenPrint(2.seconds).race(Seq(sleepThenPrint(1.seconds))
    */

object CollectAllParDemo extends zio.ZIOAppDefault:
  override def run =
    for
     durations <- ZIO.collectAllPar(
        Seq(
          sleepThenPrint(2.seconds),
          sleepThenPrint(1.seconds),
        )
      )
     total = durations.fold(Duration.Zero)(_ + _).render
     _ <- Console.printLine(total)
    yield ()