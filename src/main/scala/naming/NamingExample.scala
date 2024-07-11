package naming

import zio.*
import zio.Console.*
import zio.direct.*

case class X():
  val display = printLine("X.display")

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
  val display = printLine("Y.display")

val makeY =
  defer:
    printLine("makeY.run creating Y()").run
    Y()

object Y:
  val dependency =
    ZLayer.fromZIO:
      makeY

def _type(obj: Any): String =
  obj.getClass.getName.split("\\$")(0)

object makeYTest extends ZIOAppDefault:
  def run =
    defer:
      printLine(s"makeY: ${_type(makeY)}").run
      val r = makeY.run
      printLine(s"makeY.run returned $r").run
      printLine(s"Y.dependency: ${_type(Y.dependency)}").run

      val program =
        ZIO.serviceWithZIO[Y]:
          y =>
            defer:
              printLine(s"y: $y").run
              y.display.run
        .provide:
          Y.dependency

      printLine(s"program: ${_type(program)}").run
      program.run
      printLine("program.run complete").run