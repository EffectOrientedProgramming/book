package time

import java.time.{Instant, Period}
import zio.{UIO, ZIO, ZIOAppDefault}

object OutOfSync

case class User(name: String)
case class Post(content: String)
case class Summary(numberOfPosts: Int)

case class UserUI(
    user: User,
                          summary: Summary,
                          transactionDetails: Seq[Post]
                        ):
  val summaryCountVsDerivedCount =
    s"Summary Count: ${summary.numberOfPosts}\nDerived Count: ${transactionDetails.size}"

case class TransactionDetails(
                               transactions: Seq[Post]
                             )

val frop = User("Frop")
val zeb   = User("Zeb")
val shtep = User("Shtep")
val cheep = User("Cheep")

object TimeIgnorant:
  def summaryFor(participant: User): UIO[Summary] =
    ZIO.succeed(
      Summary(1)
    )

  def transactionsFor(participant: User): UIO[Seq[Post]] =
    ZIO.succeed(
      Seq(
        Post("Hello!"),
        Post("Goodbye!"),
      )
    )

object DemoSyncIssues extends ZIOAppDefault:
  def run =
    for
      summary <- TimeIgnorant.summaryFor(shtep)
      transactions <- TimeIgnorant.transactionsFor(shtep)
      uiContents = UserUI(shtep, summary, transactions)
      _ <- zio.Console.printLine(uiContents)
    yield ()
