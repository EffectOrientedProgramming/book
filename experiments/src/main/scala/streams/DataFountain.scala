package streams

import zio.*
import zio.metrics.MetricKeyType.Counter
import zio.stream.*

case class DataFountain(tweets: TweetStream, commitStream: CommitStream, httpRequestStream: HttpRequestStream)

object DataFountain:
  val live =
    DataFountain(
      TweetStream.Live,
      CommitStream.Live,
      HttpRequestStream.Live
    )

      // TODO More throttle investigation
//      tweets.throttleEnforce(1, 1.second, 1)(_.length)

object DemoDataFountain extends ZIOAppDefault:
  def run =
    DataFountain.live
//      .tweets.slowTweetStream
//      .commitStream.commits
      .httpRequestStream.requests
      .take(5)
      .debug
      .runDrain





