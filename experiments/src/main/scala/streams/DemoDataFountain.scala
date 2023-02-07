package streams

import zio._

object DemoDataFountain extends ZIOAppDefault:
  def run =
    DataFountain.live.commitStream
      .commits
      .take(10)
      .foreach(ZIO.debug(_))






























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
