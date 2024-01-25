package composability

import scala.concurrent.Future
import scala.util.Try

object Implementations:
  val newsService =
    new NewsService:
      def getHeadline(): Future[String] =
        Future.successful:
          "The stock market is crashing!"

  val contentAnalyzer =
    new ContentAnalyzer:
      override def findTopicOfInterest(content: String): Option[String] =
        Option
          .when(content.contains("stock market")):
            "stock market"

  val historicalRecord =
    new HistoricalRecord:
      override def summaryFor(topic: String): Either[NoRecordsAvailable, DetailedHistory] =
        topic match
          case "stock market" =>
            Right(DetailedHistory("detailed history"))
          case "TODO_OTHER_MORE_OBSCURE_TOPIC" =>
            Left(NoRecordsAvailable("blah blah"))
  val closeableFile =
    new CloseableFile:
      override def close =
        println("Closing file now!")

      override def existsInFile(
                                 searchTerm: String
                               ): Boolean = searchTerm == "stock market"

      override def write(entry: String): Try[Unit] =
        println("Writing to file: " + entry)
        if (entry == "stock market")
          Try(throw new Exception("Stock market already exists!"))
        else
          Try(())
        
