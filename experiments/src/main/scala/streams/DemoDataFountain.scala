package streams

import zio._

object DemoDataFountain extends ZIOAppDefault:
  def run =
    DataFountain
      .live
//        .tweets.tweets
//        .filter(_.text.contains("best"))
//      .commitStream.commits
      .httpRequestStream
      .requests
      .schedule(Schedule.spaced(1.second))
      //      .filter(_.response == Code.Ok)
      .take(5)
      .debug
      .runDrain

object RecognizeBurstOfBadRequests extends ZIOAppDefault:
  def run =
    DataFountain.live.httpRequestStream
      .requests
      .groupedWithin(10, 1.second)
      .debug
      .foreach( requests =>
        ZIO.when(
          requests.filter( r => r.response == Code.Forbidden).length > 2
        )(ZIO.debug("Too many bad requests")))
      .timeout(5.seconds)
