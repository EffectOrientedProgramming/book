package javawrappers

import zio.duration2DurationOps
import java.time.Instant

object InstantOps:
  extension (i: Instant)
    def plusZ(duration: zio.Duration): Instant =
      i.plus(duration.asJava).nn
