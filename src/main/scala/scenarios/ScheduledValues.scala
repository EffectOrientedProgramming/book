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
 * seconds, I would get "Third Value" 14+
 * seconds, it would fail */

object ScheduledValues:

  def scheduledValues[T](
      list: List[(Duration, T)]
  ): ZIO[Has[Clock], TimeoutException, T] = ???

  scheduledValues(
    List(
      (
        Duration(1, TimeUnit.SECONDS),
        "First Value"
      ),
      (
        Duration(3, TimeUnit.SECONDS),
        "Second Value"
      ),
      (
        Duration(10, TimeUnit.SECONDS),
        "Third Value"
      )
    )
  )
end ScheduledValues

object Scheduled2:

  private def access[A](
      timeTable: Seq[(Long, A)]
  ): ZIO[Has[Clock], TimeoutException, A] =
    for
      clock <- ZIO.environment[Has[Clock]]
      now   <- clock.get[Clock].nanoTime
      result <-
        ZIO.getOrFailWith(
          new TimeoutException("TOO LATE")
        ) {
          timeTable.find(_._1 >= now).map(_._2)
        }
    yield result

  def scheduledValues[A](
      value: (Duration, A),
      values: (Duration, A)*
  ): ZIO[Has[Clock], Nothing, ZIO[Has[
    Clock
  ], TimeoutException, A]] =
    for
      clock     <- ZIO.environment[Has[Clock]]
      startTime <- clock.get[Clock].nanoTime
      timeTable =
        values.scanLeft(
          (
            startTime + value._1.toNanos,
            value._2
          )
        ) {
          case (
                (elapsed, _),
                (duration, value)
              ) =>
            (elapsed + duration.toNanos, value)
        }
    yield access(timeTable.toList)

end Scheduled2
