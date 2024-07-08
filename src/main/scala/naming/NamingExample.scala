package naming

import zio.*
import zio.Console.*
import zio.direct.*

case class X():
  val f =
    defer:
      printLine("X.f").run

val make =
  defer:
    printLine("Creating X").run
    X()

object X:
  val made =
    ZLayer.fromZIO:
      make

object NamingExample extends ZIOAppDefault:
  def run =
    ZIO
      .serviceWithZIO[X]:
        x => x.f
      .provide:
        X.made
