package streams

import zio.*
import zio.stream.*

object HelloStreams extends ZIOAppDefault:
  def run =
    for
      _ <- ZIO.debug("Stream stuff!")
      greetingStream = ZStream.repeatWithSchedule("Hi", Schedule.spaced(1.seconds))
      insultStream = ZStream.repeatWithSchedule("Dummy", Schedule.spaced(2.seconds))
      combinedStream = ZStream.mergeAllUnbounded()(greetingStream, insultStream)
      aFewElements = combinedStream.take(6)
      res <- aFewElements.runCollect
      _ <- ZIO.debug("Res: " + res)
    yield ()
