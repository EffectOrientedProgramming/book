package time

import java.time.{Instant, Period}
import zio.{IO, UIO, ZIO, ZIOAppDefault}

object OutOfSync

// TODO Consider deduping User throughout the book
case class User(name: String)
case class Post(content: String)
case class Summary(numberOfPosts: Int)

case class UserUI(
    user: User,
    summary: Summary,
    transactionDetails: Seq[Post]
)

case class TransactionDetails(
    transactions: Seq[Post]
)

val frop  = User("Frop")
val zeb   = User("Zeb")
val shtep = User("Shtep")
val cheep = User("Cheep")

object TimeIgnorant:
  private var summaryCalledTime
      : Option[Instant] = None
  def summaryFor(
      participant: User
  ): UIO[Summary] =
    summaryCalledTime match
      case Some(value) =>
        ()
      case None =>
        summaryCalledTime =
          Some(Instant.now().nn)

    ZIO.succeed(Summary(1))

  def postsBy(
      participant: User
  ): IO[String, Seq[Post]] =
    val executionTimeStamp = Instant.now()
    for
      _ <-
        ZIO
          .getOrFailWith(
            "Must call summary before posts"
          )(summaryCalledTime)
          .flatMap(timeStamp =>
            ZIO.debug(
              "Summary called: " + timeStamp
            )
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
      summary <- TimeIgnorant.summaryFor(shtep)
      transactions <- TimeIgnorant.postsBy(shtep)
      uiContents =
        UserUI(shtep, summary, transactions)
      _ <- zio.Console.printLine(uiContents)
    yield ()
