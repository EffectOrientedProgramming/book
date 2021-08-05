package scenarios

import zio.Duration
import zio.Clock
import zio.Has
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
object Scheduled2:

  def scheduledValues[A](
      value: (Duration, A),
      values: (Duration, A)*
  ): ZIO[
    Has[Clock], // construction time
    Nothing,
    ZIO[
      Has[Clock], // access time
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
  private def createTimeTableX[A](
      startTime: Instant,
      value: (Duration, A),
      values: (Duration, A)*
  ): Seq[(Instant, A)] =
    values.scanLeft(
      (startTime.plusZ(value._1), value._2)
    ) { case ((elapsed, _), (duration, value)) =>
      (elapsed.plusZ(duration), value)
    }

  case class TimeTable(
                      valuesAndExperiations:  Seq[(Instant, A)]
                      ):


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
  private def accessX[A](
      timeTable: Seq[(Instant, A)]
  ): ZIO[Has[Clock], TimeoutException, A] =
    for
      now <- Clock.instant
      result <-
        ZIO.getOrFailWith(
          new TimeoutException("TOO LATE")
        ) {
          timeTable
            .find(_._1.isAfter(now))
            .map(_._2)
        }
    yield result

end Scheduled2
