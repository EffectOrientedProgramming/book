package zioBasics

import java.io
import zio._
import zio.Console._

import java.io.IOException

object Equality extends zio.App:

  // Equality may be non-intuative when it comes
  // to ZIO.
  // suc(1-5) are all equivalant. They all equal
  // a success with a return of 1.
  // (Their types can be aliased to be more and
  // more specific.)
  val suc1 = ZIO.succeed(1)

  val suc2: ZIO[Any, Nothing, Int] =
    ZIO.succeed(1)
  val suc3: IO[Nothing, Int] = ZIO.succeed(1)
  val suc4: UIO[Int]         = ZIO.succeed(1)

  val suc4d: UIO[Int] =
    ZIO.succeed(1) // (Duplicate of suc4)
  val suc5: URIO[Any, Int] = ZIO.succeed(1)

  // Here, we test the equality values of all the
  // ZIO:
  val myAppLogic
      : ZIO[Has[Console], IOException, Unit] =
    for
      res1: Int <-
        suc1 // Flat map all the ZIO into their integer values
      res2: Int <- suc2
      res3: Int <- suc3
      res4: Int <- suc4
      res5: Int <- suc5
      _ <-
        printLine(s"""
                       | res1: ${res1}
                       | res2: ${res2}
                       | res3: ${res3}
                       | res4: ${res4}
                       | res5: ${res5}
                       | """.stripMargin)
      // Test if the flat mapped ZIO are
      // equivelant:
      _ <-
        printLine(
          (
            res1 == res2 && res2 == res3 &&
              res3 == res4 && res4 == res5
          ).toString
        )
      // Test if the differently aliased ZIO are
      // considered equivelant:
      _ <-
        printLine(
          (
            suc1 == suc2 && suc2 == suc3 &&
              suc3 == suc4 && suc4 == suc5
          ).toString
        )
      // Test if identically defined ZIO are
      // considered equivelant:
      _ <- printLine((suc4 == suc4d).toString)
    yield ()

  // Until the ZIO are run, they cannot be
  // considered equivelant to other ZIO. Becuase
  // they represent a certain level of
  // uncertainty, their exact value cannot be
  // determined.

  def run(args: List[String]) =
    myAppLogic.exitCode
end Equality
