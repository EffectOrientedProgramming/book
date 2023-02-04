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

