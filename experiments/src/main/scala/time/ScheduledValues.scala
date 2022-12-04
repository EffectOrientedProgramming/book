package time

import zio.Duration
import zio.Clock
import zio.ZIO
import zio.URIO
import zio.Schedule
import zio.ExitCode
import zio.durationInt

import java.util.concurrent.TimeUnit
import java.time.Instant
import scala.concurrent.TimeoutException

import javawrappers.InstantOps.plusZ

/* Goal: If I accessed this from:
 * 0-1 seconds, I would get "First Value" 1-4
 * seconds, I would get "Second Value" 4-14
 * seconds, I would get "Third Value" 14+
 * seconds, it would fail */

// TODO Consider TimeSequence as a name
def scheduledValues[A](
    value: (Duration, A),
    values: (Duration, A)*
): ZIO[
  Any, // construction time
  Nothing,
  ZIO[
    Any, // access time
    TimeoutException,
    A
  ]
] =
  for
    startTime <- Clock.instant
    timeTable =
      createTimeTableX(
        startTime,
        value,
        values* // Yay Scala3 :)
      )
  yield accessX(timeTable)

// TODO Some comments, tests, examples, etc to
// make this function more obvious
private[time] def createTimeTableX[A](
    startTime: Instant,
    value: (Duration, A),
    values: (Duration, A)*
): Seq[ExpiringValue[A]] =
  values.scanLeft(
    ExpiringValue(
      startTime.plusZ(value._1),
      value._2
    )
  ) {
    case (
          ExpiringValue(elapsed, _),
          (duration, value)
        ) =>
      ExpiringValue(
        elapsed.plusZ(duration),
        value
      )
  }

/** Input: (1 minute, "value1") (2 minute,
  * "value2")
  *
  * Runtime: Zero value: (8:00 + 1 minute,
  * "value1")
  *
  * case ((8:01, _) , (2.minutes, "value2")) =>
  * (8:01 + 2.minutes, "value2")
  *
  * Output: ( ("8:01", "value1"), ("8:03",
  * "value2") )
  */
private[time] def accessX[A](
    timeTable: Seq[ExpiringValue[A]]
): ZIO[Any, TimeoutException, A] =
  for
    now <- Clock.instant
    result <-
      ZIO.getOrFailWith(
        new TimeoutException("TOO LATE")
      ) {
        timeTable
          .find(_.expirationTime.isAfter(now))
          .map(_.value)
      }
  yield result

private case class ExpiringValue[A](
    expirationTime: Instant,
    value: A
)
