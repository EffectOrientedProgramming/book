package time

import java.time.{Duration, Instant, Period}
import zio.{IO, UIO, ZIO, ZIOAppDefault}

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
    for
      timeStamp <-
        ZIO
          .getOrFailWith(
            "Must call summary before posts"
          )(summaryCalledTime)
      _ <-
        ZIO.debug(
          "Summary called: " + timeStamp
        )
      _ <- ZIO.when(Duration.between(timeStamp, Instant.now).compareTo(Duration.ofSeconds(1)) > 0)(
        ZIO.debug("Significant delay between calls. Results are skewed!")
      )
      _ <-
        ZIO.debug(
          "Getting posts:  " + executionTimeStamp
        )
    yield Seq(Post("Hello!"), Post("Goodbye!"))
  end postsBy
end TimeIgnorant

object DemoSyncIssues extends ZIOAppDefault:
  def run =
    for
      summary <- TimeIgnorant.summaryFor()
      transactions <- TimeIgnorant.postsBy()
      uiContents =
        UserUI(summary, transactions)
      _ <- zio.Console.printLine(uiContents)
    yield ()
