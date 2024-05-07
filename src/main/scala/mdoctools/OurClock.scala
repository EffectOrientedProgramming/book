package mdoctools

import zio.Clock.ClockLive

import java.time
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId}
import java.util.concurrent.TimeUnit

// provides a clock that takes no time, but reports via nanoTime that things took time based on a sleep or adjust
// not thread safe but could maybe be use a Ref to be thread safe
class OurClock(useLive: Boolean = false) extends TestClock:
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
    if useLive then
      ClockLive.nanoTime
    else
      ClockLive.nanoTime.map: t =>
        sleeper.fold(t): s =>
          val newNanos = s.toNanos + t
          sleeper = None
          newNanos

  override def scheduler(implicit trace: Trace): UIO[Scheduler] =
    ClockLive.scheduler

  override def sleep(duration: => zio.Duration)(implicit trace: Trace): UIO[Unit] =
    if useLive then
      ClockLive.sleep(duration)
    else
      sleeper = Some(duration)
      // we can't return immediately because things like timeout race
      // and if we are racing against another sleep, things get indeterminate
      // and we want to reduce very long sleeps to not take very long
      // but if we reduce all sleeps by a large number then the granularity between them is too small
      if duration > 1.minute then
        ClockLive.sleep(1.millisecond)
      else
        ClockLive.sleep(duration.dividedBy(1_000))

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
