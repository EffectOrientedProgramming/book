package toolstesting

import zio.*
import zio.ZIO.*
import zio.Console.*
import zio.direct.*
import zio.test.*

trait Foo:
  val s: String = "Hodor"

class Bar extends Foo:
  override val s = "Hold the door"

trait Tool:
  val action: String
  val act = printLine(s"$this $action")

trait Nailer extends Tool:
  val action = "nailing"
case class Hammer() extends Nailer
case class NailGun() extends Nailer

object Nailer:
  val hand = ZLayer.succeed(Hammer())
  val power = ZLayer.succeed(NailGun())

trait Saw extends Tool:
  val action = "sawing"
case class HandSaw() extends Saw
case class PowerSaw() extends Saw

object Saw:
  val hand = ZLayer.succeed(HandSaw())
  val power = ZLayer.succeed(PowerSaw())

trait Drill extends Tool:
  val action = "drilling"
case class HandDrill() extends Drill
case class PowerDrill() extends Drill

object Drill:
  val hand = ZLayer.succeed(HandDrill())
  val power = ZLayer.succeed(PowerDrill())

object AllTools extends ZIOAppDefault:
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

val allTools = List(Saw.hand, Saw.power, Nailer.hand, Nailer.power/*, Drill.hand, Drill.power */)
val allMaterials = List(wood, metal /*, plastic */)

// invisible
def allCombinations[A, B](seq1: Seq[A], seq2: Seq[B]): Seq[(A, B)] =
  seq1.flatMap(elem1 => seq2.map(elem2 => (elem1, elem2)))
//

def toolCombinations(tools: Seq[ULayer[Tool]]) =
  defer:
    ZIO.foreach(allCombinations(tools, allMaterials)):
      case (tool, material) =>
        defer:
          val tool = ZIO.service[Tool].run
          val material = ZIO.service[Material].run
          use(tool, material).run
        .provide(tool, material)
    .run

object MaterialWithTool extends ZIOAppDefault:
  def run =
    toolCombinations(allTools)


object TestTools extends ZIOSpecDefault:
  def spec =
    suite("Tools")(
      zio.test.test("Hand tools") {
        toolCombinations(List(Saw.hand, Nailer.hand))
        assertCompletes
      },
      zio.test.test("Power tools") {
        toolCombinations(List(Saw.power, Nailer.power))
        assertCompletes
        // assertNever("power test failed")
      },
    )
