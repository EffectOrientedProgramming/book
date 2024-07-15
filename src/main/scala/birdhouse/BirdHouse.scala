package birdhouse

import zio.*

trait Material:
  val brittleness: Int

case class Wood() extends Material:
  val brittleness = 5

case class Plastic() extends Material:
  val brittleness = 10

object Material:
  val wood    = ZLayer.succeed(Wood())
  val plastic = ZLayer.succeed(Plastic())

import zio.Console.*
import zio.direct.*

trait Tool:
  val action: String
  val intensity: Int
  val use =
    printLine(
      s"$this $action, intensity $intensity"
    )

trait Saw extends Tool:
  val action = "sawing"
case class HandSaw() extends Saw:
  val intensity = 4
case class RoboSaw() extends Saw:
  val intensity = 8

object Saw:
  val hand    = ZLayer.succeed(HandSaw())
  val robotic = ZLayer.succeed(RoboSaw())

trait Nailer extends Tool:
  val action = "nailing"
case class Hammer() extends Nailer:
  val intensity = 4
case class RoboNailer() extends Nailer:
  val intensity = 11

object Nailer:
  val hand    = ZLayer.succeed(Hammer())
  val robotic = ZLayer.succeed(RoboNailer())

// Just checking
object UseTools extends ZIOAppDefault:
  def run =
    defer:
      List(
        Hammer(),
        RoboNailer(),
        HandSaw(),
        RoboSaw(),
      ).foreach(_.use.run)
// end checking

import zio.test.*

val testToolWithMaterial =
  defer:
    val saw      = ZIO.service[Saw].run
    val nailer   = ZIO.service[Nailer].run
    val material = ZIO.service[Material].run
    assertTrue(
      saw.intensity < material.brittleness,
      nailer.intensity < material.brittleness,
    )

object BirdHouseTest extends ZIOSpecDefault:
  def spec =
    suite("Tools and BuildingMaterials")(
      test("Hand tools & Wood"):
        testToolWithMaterial.provide(
          Saw.hand,
          Nailer.hand,
          Material.wood,
        )
      ,
      test("Hand tools & Plastic"):
        testToolWithMaterial.provide(
          Saw.hand,
          Nailer.hand,
          Material.plastic,
        )
      ,
      test("Robo tools & Plastic"):
        testToolWithMaterial.provide(
          Saw.robotic,
          Nailer.robotic,
          Material.plastic,
        )
      ,
      test("Robo saw, hammer & Plastic"):
        testToolWithMaterial.provide(
          Saw.robotic,
          Nailer.hand,
          Material.plastic,
        ),
    )
end BirdHouseTest
