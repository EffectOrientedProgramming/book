package naming

import zio.*
import zio.Console.*
import zio.direct.*

case class X():
  val f = printLine("X.f")

val makeX =
  defer:
    printLine("Creating X").run
    X()

object X:
  val made =
    ZLayer.fromZIO:
      makeX

object NamingExampleX extends ZIOAppDefault:
  def run =
    ZIO
      .serviceWithZIO[X]:
        x => x.f
      .provide:
        X.made

// ----------------------------------------------------

case class Y():
  val f = printLine("Y.f")

val makeY =
  defer:
    printLine("Creating Y").run
    val y = Y()
    y.f.run
    printLine(s"Inside makeY: $y").run
    y

object Y:
  val made =
    ZLayer.fromZIO:
      makeY

object makeYTest extends ZIOAppDefault:
  def run =
    defer:
      val y = makeY
      printLine(s"y = $y").run
      val r = y.run
      printLine(s"r = $r").run

      val m = Y.made
      printLine(s"m = $m").run

      val yy =
        ZIO.serviceWithZIO[Y]:
          y => printLine(s"y = $y")
        .provide:
          Y.made

      printLine(s"yy = $yy").run
      yy.run
      printLine("yy.run complete").run
