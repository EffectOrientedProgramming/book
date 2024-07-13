package tools

import zio.*
import zio.ZIO.*
import zio.Console.*
import zio.direct.*

trait Tool:
  val action: String
  val act = printLine(s"$this $action")

trait Nailer extends Tool:
  override val action = "nailing"
case class Hammer() extends Nailer
case class NailGun() extends Nailer

object Nailer:
  val hand = ZLayer.succeed(Hammer())
  val power = ZLayer.succeed(NailGun())

trait Screwer extends Tool:
  override val action = "screwing"
case class Screwdriver() extends Screwer
case class PowerScrewer() extends Screwer

object Screwer:
  val hand = ZLayer.succeed(Screwdriver())
  val power = ZLayer.succeed(PowerScrewer())

trait Drill extends Tool:
  override val action = "drilling"
case class HandDrill() extends Drill
case class PowerDrill() extends Drill

object Drill:
  val hand = ZLayer.succeed(HandDrill())
  val power = ZLayer.succeed(PowerDrill())

object testTools extends ZIOAppDefault:
  def run =
    defer:
      List(Hammer(), NailGun(), Screwdriver(), PowerScrewer(), HandDrill(), PowerDrill())
        .foreach(_.act.run)

trait Material
case class Wood() extends Material
case class Metal() extends Material
case class Plastic() extends Material

val wood = ZLayer.succeed(Wood())
val metal = ZLayer.succeed(Metal())
val plastic = ZLayer.succeed(Plastic())

def use(t: Tool, m: Material) =
  printLine(s"Using $t on $m")

val tools = List(Nailer.hand, Nailer.power, Screwer.hand, Screwer.power, Drill.hand, Drill.power)
val materials = List(wood, metal, plastic)

val combinations = for {
  tool <- tools
  material <- materials
} yield (tool, material)

object materialWithTool extends ZIOAppDefault:
  def run =
    ZIO.foreach(combinations):
      case (tool, material):
        defer:
          val tool = ZIO.service[Tool].run
          val material = ZIO.service[Material].run
          use(tool, material).run
        .provide(tool, material)
