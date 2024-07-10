package naming

import zio.*
import zio.Console.*
import zio.direct.*

case class X():
  val display = printLine("X.f")

val makeX =
  defer:
    printLine("Creating X").run
    X()

val dependency =
  ZLayer.fromZIO:
    makeX

object X:
  val dependent =
    ZLayer.fromZIO:
      makeX


object NamingExampleX extends ZIOAppDefault:
  def run =
    ZIO
      .serviceWithZIO[X]:
        x => x.display
      .provide:
        X.dependent   // The "adjectivized object"
        // dependency // Or the noun version

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
  val dependency =
    ZLayer.fromZIO:
      makeY

object makeYTest extends ZIOAppDefault:
  def run =
    defer:
      val y = makeY
      printLine(s"y = $y").run
      val r = y.run
      printLine(s"r = $r").run

      val m = Y.dependency
      printLine(s"m = $m").run

      val yy =
        ZIO.serviceWithZIO[Y]:
          y => printLine(s"y = $y")
        .provide:
          Y.dependency

      printLine(s"yy = $yy").run
      yy.run
      printLine("yy.run complete").run
