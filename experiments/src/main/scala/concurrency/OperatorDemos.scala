package concurrency

import zio.{
  durationInt,
  duration2DurationOps,
  Clock,
  Console,
  Duration,
  ZIO,
  ZIOAppDefault,
  Random
}
import zio.direct.*

def sleepThenPrint(
    d: Duration
): ZIO[Any, java.io.IOException, Duration] =
  defer {
    ZIO.sleep(d).run
    Console.printLine(s"${d.render} elapsed").run
    d
  }

object ForkDemo extends zio.ZIOAppDefault:
  override def run =
    defer {
      val f1 = sleepThenPrint(2.seconds).fork.run
      val f2 = sleepThenPrint(1.seconds).fork.run
      f1.join.run
      f2.join.run
    }

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
    ZIO.raceAll(
      sleepThenPrint(2.seconds),
      Seq(sleepThenPrint(1.seconds))
    )
    /* // alternate syntax:
     * sleepThenPrint(2.seconds).race(Seq(sleepThenPrint(1.seconds)) */

object CollectAllParDemo
    extends zio.ZIOAppDefault:
  override def run =
    defer {
      val durations =
        ZIO.collectAllPar(
          Seq(
            sleepThenPrint(2.seconds),
            sleepThenPrint(1.seconds)
          )
        ).run
      val total =
        durations
          .fold(Duration.Zero)(_ + _)
          .render
      Console.printLine(total).run
    }
end CollectAllParDemo

object CollectAllParMassiveDemo
    extends zio.ZIOAppDefault:
  override def run =
    defer {
      val durations =
        ZIO.collectAllSuccessesPar(
          Seq
            .fill(1_000_000)(1.seconds)
            .map(duration =>
              defer {
                val randInt =
                  Random.nextIntBetween(0, 100).run
                ZIO.sleep(duration).run
                ZIO.when(randInt < 10)(
                  ZIO.fail("Number is too low")
                ).run
                duration
              }
            )
        ).run
      val total =
        durations
          .fold(Duration.Zero)(_ + _)
          .render
      Console.printLine(total).run
    }

end CollectAllParMassiveDemo
