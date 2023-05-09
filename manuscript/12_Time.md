# Time

Time based functions are effectful because they
rely on a variable that is constantly changing.


Your program displays 2 sections:
    Summary
        -Time range
        -totalNumberOfTransactions 
        -All Participants

    Details
        - List[Transaction]
        
Show how these can be out of sync with unprincipled `Clock` access

`.now()`

How often it is overlooked/minimized
"Race Condition" vs "race operation"
Example possibilities
    - Progress bar
    - query(largeRange) followed by query(smallRange), and getting new results in the 2nd call

## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/time/OutOfSync.scala
```scala
package time

import java.time.{Duration, Instant, Period}
import zio.*
import zio.direct.*

// TODO Consider deduping User throughout the book
case class Post(content: String)
case class Summary(numberOfPosts: Int)

case class TransactionDetails(
    transactions: Seq[Post]
)

object User:
  case class User(name: String)
  val frop  = User("Frop")
  val zeb   = User("Zeb")
  val shtep = User("Shtep")
  val cheep = User("Cheep")

import time.User.*

case class UserUI(
    summary: Summary,
    transactionDetails: Seq[Post]
)

object TimeIgnorant:
  private var summaryCalledTime
      : Option[Instant] = None
  def summaryFor(): UIO[Summary] =
    summaryCalledTime match
      case Some(value) =>
        ()
      case None =>
        summaryCalledTime = Some(Instant.now())

    ZIO.succeed(Summary(1))

  def postsBy(): IO[String, Seq[Post]] =
    val executionTimeStamp = Instant.now()
    defer {
      val timeStamp =
        ZIO
          .getOrFailWith(
            "Must call summary before posts"
          )(summaryCalledTime)
          .run
      ZIO
        .debug("Summary called: " + timeStamp)
        .run
      ZIO
        .when(
          Duration
            .between(timeStamp, Instant.now)
            .compareTo(Duration.ofSeconds(1)) > 0
        )(
          ZIO.debug(
            "Significant delay between calls. Results are skewed!"
          )
        )
        .run
      ZIO
        .debug(
          "Getting posts:  " + executionTimeStamp
        )
        .run
      Seq(Post("Hello!"), Post("Goodbye!"))
    }
  end postsBy
end TimeIgnorant

object DemoSyncIssues extends ZIOAppDefault:
  def run =
    defer {
      val summary = TimeIgnorant.summaryFor().run
      val transactions =
        TimeIgnorant.postsBy().run
      val uiContents =
        UserUI(summary, transactions)
      zio.Console.printLine(uiContents).run
    }

```


### experiments/src/main/scala/time/ScheduledValues.scala
```scala
package time

import zio.*
import zio.direct.*

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
  defer {
    val startTime = Clock.instant.run
    val timeTable =
      createTimeTableX(
        startTime,
        value,
        values* // Yay Scala3 :)
      )
    accessX(timeTable)
  }

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
  defer {
    val now = Clock.instant.run
    ZIO
      .getOrFailWith(
        new TimeoutException("TOO LATE")
      ) {
        timeTable
          .find(_.expirationTime.isAfter(now))
          .map(_.value)
      }
      .run
  }

private case class ExpiringValue[A](
    expirationTime: Instant,
    value: A
)

```


### experiments/src/main/scala/time/TimedTapTap.scala
```scala
package time

import zio.*
import zio.Console.*

val longRunning =
  ZIO.sleep(5.seconds) *> printLine("done")

val runningNotifier =
  (
    ZIO.sleep(1.seconds) *>
      printLine("Still running")
  ).onInterrupt {
    printLine("finalized").orDie
  }

object TimedTapTapJames extends ZIOAppDefault:

  def run =
    for
      lr <- longRunning.fork
      _  <- runningNotifier.fork
      _  <- lr.join
    yield ()

object TimedTapTapBill extends ZIOAppDefault:
  def run =
    longRunning
      .race(runningNotifier *> ZIO.never)

```

