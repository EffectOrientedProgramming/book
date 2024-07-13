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

trait Saw extends Tool:
  override val action = "sawing"
case class HandSaw() extends Saw
case class PowerSaw() extends Saw

object Saw:
  val hand = ZLayer.succeed(HandSaw())
  val power = ZLayer.succeed(PowerSaw())

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
      List(Hammer(), NailGun(), HandSaw(), PowerSaw(), HandDrill(), PowerDrill())
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

val tools = List(Saw.hand, Saw.power, Nailer.hand, Nailer.power/*, Drill.hand, Drill.power */)
val materials = List(wood, metal /*, plastic */)

//val combinations = for {
//  tool <- tools
//  material <- materials
//} yield (tool, material)

// invisible
def allCombinations[A, B](list1: List[A], list2: List[B]): List[(A, B)] =
  list1.flatMap(elem1 => list2.map(elem2 => (elem1, elem2)))
//

object materialWithTool extends ZIOAppDefault:
  def run =
    ZIO.foreach(allCombinations(tools, materials)):
      case (tool, material) =>
        defer:
          val tool = ZIO.service[Tool].run
          val material = ZIO.service[Material].run
          use(tool, material).run
        .provide(tool, material)

import zio.test.*

val handTools = List(Saw.hand, Nailer.hand)
val powerTools = List(Saw.power, Nailer.power)


object TestTools extends ZIOSpecDefault:
  def spec =
    suite("Tools")(
      zio.test.test("hand") {
        defer:
          ZIO.foreach(allCombinations(handTools, materials)):
            case (tool, material) =>
              defer:
                val tool = ZIO.service[Tool].run
                val material = ZIO.service[Material].run
                use(tool, material).run
              .provide(tool, material)
          .run
          assertCompletes
      },
      zio.test.test("power") {
        defer:
          ZIO.foreach(allCombinations(powerTools, materials)):
            case (tool, material) =>
              defer:
                val tool = ZIO.service[Tool].run
                val material = ZIO.service[Material].run
                use(tool, material).run
              .provide(tool, material)
          .run
          assertCompletes
      },
    )
