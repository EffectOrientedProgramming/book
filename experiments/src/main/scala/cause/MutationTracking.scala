package cause

import zio.{Cause, IO, UIO, ZIO}
import zio.Console.*

class MutationTracking:
  enum Stage:
    case Hominini,
      Chimpanzee,
      Human


object TimelineFinally extends App:
  try {
    "Everything went fine"
//    throw new Exception("Straightened Spine")
  } finally {
    try {
      throw new Exception("Less Hair")
    } finally {
      throw new Exception("Fine Voice Control")
    }
  }

object Timeline extends zio.ZIOAppDefault:
  val mutation1: UIO[Nothing] =
    ZIO.die(Exception("Straightened Spine"))
  val mutation2 = ZIO.die(Exception("Less Hair"))
  val mutation3 =
    ZIO.die(Exception("Fine voice control"))

  val timeline =
    (mutation1
      .ensuring(mutation2)
      .ensuring(mutation3)
  )
      // .sandbox
//      .catchAll { case cause: Cause[String] =>
//        printLine(cause.defects)
//      }

//  val failable: ZIO[Any, String, Nothing] = ZIO.fail("boom")
  val timelineSandboxed: ZIO[Any, Cause[String], Nothing] =
    timeline.sandbox

  def run =
    timeline.sandbox
end Timeline


