package streams

import zio.*
import zio.metrics.MetricKeyType.Counter
import zio.stream.*

case class DataFountain(
    tweets: TweetStream,
    commitStream: CommitStream,
    httpRequestStream: HttpRequestStream,
    rate: Schedule[Any, Nothing, Long] = Schedule.spaced(1.second)
):
  def withRate(newValue: Int) = copy(rate = Schedule.spaced(1.second.dividedBy(newValue)))


object DataFountain:

  def userFriendlyConstructor(rate: Int) =
    DataFountain(
      TweetStream.Live,
      CommitStream.Live,
      HttpRequestStream.Live,
      Schedule.spaced(1.second.dividedBy(rate))
    )

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
    DataFountain
      .live
//        .tweets.tweets
//        .filter(_.text.contains("best"))
//      .commitStream.commits
      .httpRequestStream .requests
      .schedule(Schedule.spaced(1.second))
      //      .filter(_.response == Code.Ok)
//      .take(5)
      .debug
      .runDrain


object DemoDataFountain2 extends ZIOAppDefault:
  def run =
    DataFountain.live.tweets.slowTweetStream
      .debug
      .runDrain


object RecognizeBurstOfBadRequests extends ZIOAppDefault:
  def run =
    DataFountain.live.httpRequestStream
        .requests
        .groupedWithin(10, 1.second)
      .tap( requests =>
        ZIO.when(
          requests.filter( r => r.response == Code.Forbidden).length > 2
        )(ZIO.debug("Too many bad requests")))
//        .map(_.count(_.status == 500))
        .debug
        .runDrain
        .timeout(5.seconds)
