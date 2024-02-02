package composability

import zio.*

import scala.concurrent.Future
import zio.direct.*

import scala.Option
import scala.util.Try

trait NewsService:
  def getHeadline(): Future[String]

case class NoInterestingTopicsFound()
trait ContentAnalyzer:
  def findTopicOfInterest(
      content: String
  ): Option[String]

case class NoRecordsAvailable(topic: String)
trait HistoricalRecord:

  def summaryFor(
      topic: String
  ): Either[NoRecordsAvailable, String]

trait CloseableFile extends AutoCloseable:
  def existsInFile(searchTerm: String): Boolean

  def write(entry: String): Try[Unit]

case class Scenario(
    newsService: NewsService,
    contentAnalyzer: ContentAnalyzer,
    historicalRecord: HistoricalRecord,
    closeableFile: CloseableFile
):
  val headLineZ =
    ZIO
      .from:
        newsService.getHeadline()

  def topicOfInterestZ(headline: String) =
    ZIO
      .from:
        contentAnalyzer.findTopicOfInterest:
          headline
      .orElseFail:
        NoInterestingTopicsFound()
        
  val closeableFileZ =
    ZIO
      .fromAutoCloseable:
        ZIO.succeed:
          closeableFile
  
  def summaryForZ(topic: String) =
    ZIO
      .from:
        historicalRecord.summaryFor:
          topic
          
  def writeToSummaryFileZ(summaryFile: CloseableFile, content: String) =
    ZIO
      .from:
        summaryFile.write:
          content

  val logic =
    defer:
      val headline: String =
        headLineZ.run

      val topic: String =
        topicOfInterestZ(headline).run

      val summaryFile: CloseableFile =
        closeableFileZ.run

      val topicIsFresh: Boolean =
        summaryFile.existsInFile:
          topic

      if (topicIsFresh)
        val newInfo =
          summaryForZ(topic).run
          
        writeToSummaryFileZ(summaryFile, newInfo)
          .run

      ZIO
        .debug:
          "topicIsFresh: " + topicIsFresh
        .run

      // todo: some error handling to show that
      // the errors weren't lost along the way
    .catchAll:
      case _: Throwable =>
        ZIO.debug("News Service could not fetch the latest headline")
      case NoRecordsAvailable(topic) =>
        ZIO.debug(s"Could not generate a summary for $topic")
      case NoInterestingTopicsFound() =>
        ZIO.debug(s"No Interesting topic found in the headline")
end Scenario

object AllTheThings extends ZIOAppDefault:
  type Nail = ZIO.type
  /* If ZIO is your hammer, it's not that you
   * _see_ everything as nails.
   * You can actually _convert_ everything into
   * nails. */

  /* Is Either different enough to demo here?
   * It basically splits the difference between
   * Option/Try I think if we show both of them,
   * we can skip Either. */

  override def run =
    defer:
      val scenario = ZIO.service[Scenario].run
      scenario.logic
    .provide(
      ZLayer.fromFunction(Scenario.apply),
      ZLayer.succeed(Implementations.newsService),
      ZLayer.succeed(Implementations.contentAnalyzer),
      ZLayer.succeed(Implementations.historicalRecord),
      ZLayer.succeed(Implementations.closeableFile)
    )
end AllTheThings
