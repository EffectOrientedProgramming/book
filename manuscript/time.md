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

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/time.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/time/OutOfSync.scala
```scala
package time

import java.time.{Duration, Instant}

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
          ):
            summaryCalledTime
          .run
      ZIO
        .debug:
          "Summary called: " + timeStamp
        .run
      ZIO
        .when(
          Duration
            .between(timeStamp, Instant.now)
            .compareTo(Duration.ofSeconds(1)) > 0
        ):
          ZIO.debug:
            "Significant delay between calls. Results are skewed!"
        .run
      ZIO
        .debug:
          "Getting posts:  " + executionTimeStamp
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


### experiments/src/main/scala/time/TimedTapTap.scala
```scala
package time

val longRunning =
  Console.printLine("done").delay(5.seconds)

val runningNotifier =
  defer:
    ZIO.sleep(1.seconds).run
    Console.printLine("Still running").run

object TimedTapTapJames extends ZIOAppDefault:

  def run =
    defer:
      val lr = longRunning.fork.run
      runningNotifier.fork.run
      lr.join.run

```

