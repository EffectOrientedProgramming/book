package time

import java.time.{Duration, Instant, Period}

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
