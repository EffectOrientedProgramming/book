package mdoctools

import zio.Clock.ClockLive

import java.time
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId}
import java.util.concurrent.TimeUnit

// provides a clock that takes no time, but reports via nanoTime that things took time based on a sleep or adjust
// not thread safe but could maybe be use a Ref to be thread safe
object OurClock extends TestClock:
  var sleeper: Option[Duration] = None

  override def currentTime(unit: => TimeUnit)(implicit trace: Trace): UIO[Long] =
    ClockLive.currentTime(unit)

  override def currentTime(unit: => ChronoUnit)(implicit trace: Trace, d: DummyImplicit): UIO[Long] =
    ClockLive.currentTime(unit)

  override def currentDateTime(implicit trace: Trace): UIO[OffsetDateTime] =
    ClockLive.currentDateTime

  override def instant(implicit trace: Trace): UIO[Instant] =
    ClockLive.instant

  override def javaClock(implicit trace: Trace): UIO[time.Clock] =
    ClockLive.javaClock

  override def localDateTime(implicit trace: Trace): UIO[LocalDateTime] =
    ClockLive.localDateTime

  override def nanoTime(implicit trace: Trace): UIO[Long] =
    ClockLive.nanoTime.map: t =>
      sleeper.fold(t): s =>
        val newNanos = s.toNanos + t
        sleeper = None
        newNanos

  override def scheduler(implicit trace: Trace): UIO[Scheduler] =
    ClockLive.scheduler

  override def sleep(duration: => zio.Duration)(implicit trace: Trace): UIO[Unit] =
    sleeper = Some(duration)
    ZIO.unit

  override def adjust(duration: zio.Duration)(implicit trace: Trace): UIO[Unit] =
    sleeper = Some(duration)
    ZIO.unit

  override def adjustWith[R, E, A](duration: zio.Duration)(zio: ZIO[R, E, A])(implicit trace: Trace): ZIO[R, E, A] = ???

  override def setTime(instant: Instant)(implicit trace: Trace): UIO[Unit] = ???

  override def setTimeZone(zone: ZoneId)(implicit trace: Trace): UIO[Unit] = ???

  override def sleeps(implicit trace: Trace): UIO[List[Instant]] = ???

  override def timeZone(implicit trace: Trace): UIO[ZoneId] = ???

  override def save(implicit trace: Trace): UIO[UIO[Unit]] =
    ZIO.succeed(ZIO.unit)
