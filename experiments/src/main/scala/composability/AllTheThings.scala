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

case class DetailedHistory(content: String)
case class NoRecordsAvailable(reason: String)
trait HistoricalRecord:

  def summaryFor(topic: String): Either[NoRecordsAvailable, DetailedHistory]

trait CloseableFile extends AutoCloseable:
  def existsInFile(searchTerm: String): Boolean

  def write(entry: String): Try[Unit]


case class Scenario(
    newsService: NewsService,
    contentAnalyzer: ContentAnalyzer,
    historicalRecord: HistoricalRecord,
    closeableFile: CloseableFile
                   ):

  val resourcefulThing =
    val open =
      defer:
        ZIO.debug("open").run
        "asdf"

    val close =
      (_: Any) =>
        ZIO.debug("close")

    ZIO.acquireRelease(open)(close)

  val logic =
    defer:
      // todo: useful order, maybe async first or
      // near first?
      // maybe something parallel in here too?
      // Convert from AutoCloseable
      // maybe add Future or make asyncThing a
      // Future `
      val headline: String =
        ZIO
          .from:
            newsService.getHeadline()
          .run

      val topic =
        ZIO
          .from:
            contentAnalyzer.findTopicOfInterest:
              headline
          .mapError(_ => NoInterestingTopicsFound())
          .run

      val summaryFileZ =
        ZIO
          .fromAutoCloseable:
            ZIO.succeed:
              closeableFile
          .run

      val topicIsFresh =
        summaryFileZ
          .existsInFile:
            topic

      if (topicIsFresh)
        val newInfo =
          ZIO.from:
            historicalRecord.summaryFor:
              topic
          .run
        ZIO.from:
          summaryFileZ.write:
            newInfo.content
        .run

      ZIO.debug:
        "topicIsFresh: " + topicIsFresh
      .run

        // todo: some error handling to show that
        // the errors weren't lost along the way

    .catchAll:
      case t: Throwable =>
        ???
      case noRecords: NoRecordsAvailable =>
        ???
      case nothing: NoInterestingTopicsFound =>
        ???


object AllTheThings extends ZIOAppDefault:
  type Nail = ZIO.type
  /* If ZIO is your hammer, it's not that you
   * _see_ everything as nails.
   * You can actually _convert_ everything into
   * nails. */

  /*  Possible scenario:
   * Get headline - Future Analyze for
   * topic/persons of interest - Option Check if
   * we have made an entry for them in today's
   * summary file - Resource If not:
   * Dig up supporting information on the topic
   * from a DB - Try Make new entry in today's
   * summary file - Resource
   *
   * Is Either different enough to demo here?
   * It basically splits the difference between
   * Option/Try I think if we show both of them,
   * we can skip Either. */


  override def run =
    Scenario(
      Implementations.newsService,
      Implementations.contentAnalyzer,
      Implementations.historicalRecord,
      Implementations.closeableFile
    ).logic
end AllTheThings