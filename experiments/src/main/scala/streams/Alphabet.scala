package streams

import zio.stream.*

object Alphabet1 extends ZIOAppDefault:

  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .debug
      .runDrain

object Alphabet2 extends ZIOAppDefault:

  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .forever
      .debug
      .runDrain

object Alphabet3 extends ZIOAppDefault:

  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .mapZIO { c =>
        defer {
          val d = Random.nextIntBounded(5).run
          ZIO.sleep(d.seconds).run
          ZIO.debug(c).run
        }.fork
      }
      .runDrain // exits before all forks are completed

object Alphabet4 extends ZIOAppDefault:
  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .schedule(
        Schedule.fixed(1.second).jittered
      )
      .aggregateAsyncWithin(
        ZSink.collectAll,
        Schedule.fixed(3.seconds)
      )
      .debug("Elements in past 3 seconds")
      .map(_.length)
      .debug("Rate per 3 seconds")
      .runDrain

// doesn't chunk into time-oriented groups as we'd expect
object Alphabet5 extends ZIOAppDefault:
  override def run =
    ZStream
      .fromIterable('a' to 'z')
      .schedule(Schedule.spaced(10.millis))
      .throttleShape(1, 1.second) { chunk =>
        println(chunk)
        1
      }
      .debug
      .runDrain

// grouping as many items as can fit in one second, with a cap of 1000
// Note: Int.MaxValue causes OOM
object Alphabet6 extends ZIOAppDefault:
  def run =
    ZStream
      .fromIterable('a' to 'z')
      .schedule(Schedule.spaced(100.millis))
      .groupedWithin(1000, 1.second)
      .debug
      .runDrain
