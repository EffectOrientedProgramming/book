package streams

import zio.*
import zio.stream.*

def specifyStream[A](
    f: DataFountain => Stream[Nothing, A]
) =
  f(DataFountain.live)
    .take(10)
    .foreach(ZIO.debug(_))

object DemoDataFountainTweets
    extends ZIOAppDefault:
  def run =
    specifyStream:
      _.tweets.tweets

object DemoDataFountainHttpRequests
    extends ZIOAppDefault:
  def run =
    specifyStream:
      _.httpRequestStream.requests

object DemoDataFountainCommits
    extends ZIOAppDefault:
  def run =
    specifyStream:
      _.commitStream.commits

object RecognizeBurstOfBadRequests
    extends ZIOAppDefault:
  def run =
    DataFountain
      .live
      .httpRequestStream
      .requests
      .groupedWithin(10, 1.second)
      .debug
      .foreach(requests =>
        ZIO.when(
          requests
            .filter: r =>
              r.response == Code.Forbidden
            .length > 2
        )(ZIO.debug("Too many bad requests"))
      )
      .timeout:
        5.seconds
end RecognizeBurstOfBadRequests
