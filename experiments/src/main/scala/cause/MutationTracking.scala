package cause

import zio.{ZEnv, ZIO}
import zio.Console._
import zio.Cause

class MutationTracking:
  enum Stage:
    case Hominini,
      Chimpanzee,
      Human

object Timeline extends zio.ZIOAppDefault:
  val mutation1 = ZIO.fail("Straightened Spine")
  val mutation2 =
    ZIO
      .fail("Less Hair")
      .orDieWith(new Exception(_))
  val mutation3 =
    ZIO
      .fail("Fine voice control")
      .orDieWith(new Exception(_))

  val timeline =
    mutation1
      .ensuring(mutation2)
      .ensuring(mutation3)
      .sandbox
      .catchAll { case cause: Cause[String] =>
        printLine(cause.defects)
      }

  def run = timeline.exitCode
end Timeline
