package runtime

import zio.{Console, ZIO, ZIOAppDefault}

import java.time.Instant

object BuiltInServices extends ZIOAppDefault:
  val logic
      : ZIO[Any, java.io.IOException, Instant] =
    for
      now <-
        ZIO.clockWith(clock => clock.instant)
      _ <- Console.printLine("Now: " + now)
    yield now

  def run = logic
