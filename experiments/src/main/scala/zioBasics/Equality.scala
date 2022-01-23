package zioBasics

import zio.*
import zio.Console.printLine

import java.io.IOException

val suc1                         = ZIO.succeed(1)
val suc2: ZIO[Any, Nothing, Int] = ZIO.succeed(1)
val suc3: IO[Nothing, Int]       = ZIO.succeed(1)

val suc4: UIO[Int]          = ZIO.succeed(1)
val suc4duplicate: UIO[Int] = ZIO.succeed(1)

val suc5: URIO[Any, Int] = ZIO.succeed(1)

val testEqualities
    : ZIO[Console, IOException, Unit] =
  for
    res1: Int <- suc1
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
    _ <-
      printLine(
        (
          res1 == res2 && res2 == res3 &&
            res3 == res4 && res4 == res5
        ).toString
      )
    _ <-
      printLine(
        suc1 == suc2 && suc2 == suc3 &&
          suc3 == suc4 && suc4 == suc5
      )
    _ <-
      printLine((suc4 == suc4duplicate).toString)
  yield ()

object Equality extends ZIOAppDefault:
  def run = testEqualities
