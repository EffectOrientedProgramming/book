package naming

import zio.*
import zio.Console.*
import zio.direct.*

case class X():
  val f = printLine("X.f")

val makeX =
  defer:
    printLine("Creating X").run
    val x = X()
    x.f.run
    printLine(s"Inside makeX: $x").run
    x

object makeXTest extends ZIOAppDefault:
  def run =
    defer:
      val x = makeX
      printLine(s"x = $x").run
      val r = x.run
      printLine(s"r = $r").run




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

case class Y():
  val f = printLine("Y.f")

val makeY =
  defer:
    printLine("Creating Y").run
    Y()

object Y:
  val made =
    ZLayer.fromZIO:
      makeY

object NamingExampleY extends ZIOAppDefault:
  def run =
    ZIO
      .serviceWithZIO[Y]:
        y => y.f
      .provide:
        Y.made

