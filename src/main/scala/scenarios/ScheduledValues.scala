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
import scala.concurrent.TimeoutException

/* Goal: If I accessed this from:
 * 0-1 seconds, I would get "First Value" 1-4
 * seconds, I would get "Second Value" 4-14
 * seconds, I would get "Third Value" 14+ *
 * seconds, it would fail */

object Scheduled2:

  def scheduledValues[A](
      value: (Duration, A),
      values: (Duration, A)*
  ): ZIO[Has[Clock], Nothing, ZIO[Has[
    Clock
  ], TimeoutException, A]] =
    for
      startTime: Long <- Clock.nanoTime
      timeTable =
        createTimeTable(
          startTime,
          value,
          values* // Yay Scala3 :)
        )
    yield access(timeTable.toList)

  // TODO Some comments, tests, examples, etc to
  // make this function more obvious
  private def createTimeTable[A](
      startTime: Long,
      value: (Duration, A),
      values: (Duration, A)*
  ) =
    values.scanLeft(
      (startTime + value._1.toNanos, value._2)
    ) { case ((elapsed, _), (duration, value)) =>
      (elapsed + duration.toNanos, value)
    }

  private def access[A](
      timeTable: Seq[(Long, A)]
  ): ZIO[Has[Clock], TimeoutException, A] =
    for
      now <- Clock.nanoTime
      result <-
        ZIO.getOrFailWith(
          new TimeoutException("TOO LATE")
        ) {
          timeTable.find(_._1 >= now).map(_._2)
        }
    yield result

end Scheduled2
