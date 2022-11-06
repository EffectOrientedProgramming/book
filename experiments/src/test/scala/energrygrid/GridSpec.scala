package energrygrid

import zio.*
import zio.test.*
import energrygrid.GridErrors.*
import zio.test.TestAspect.ignore

/* TODO Simulate a home-level grid managing power
 * needs and production Why?
 * - This is code that interacts with TheWorld in
 * numerous ways
 * - It's something I've wanted a more visceral
 * understanding of */
trait EnergyParticipant

trait EnergyProvider:
  val producingPriority: Int

sealed trait EnergyProducer
    extends EnergyParticipant
    with EnergyProvider:
  object SolarPanels
      extends EnergyProducer
      with EnergyProvider:
    override val producingPriority = 10

  object Generator
      extends EnergyProducer
      with EnergyProvider:
    override val producingPriority = 1

  def sendPowerTo(
      consumer: EnergyConsumer |
        EnergyBidirectional
  ): ZIO[Any, Overheat, Unit] = ???

sealed trait EnergyConsumer
    extends EnergyParticipant:
  val consumerPriority: Int
object EnergyConsumer:
  object Dishwasher extends EnergyConsumer:
    override val consumerPriority: Int = 3

  object Wifi extends EnergyConsumer:
    override val consumerPriority: Int = 4

  object Refrigerator extends EnergyConsumer:
    override val consumerPriority: Int = 5

  def drawPowerFrom(
      producer: EnergyProducer |
        EnergyBidirectional
  ): ZIO[Any, InsufficientPower, Unit] = ???

sealed trait EnergyBidirectional
    extends EnergyProvider
    with EnergyConsumer:
  def sendPowerTo(
      consumer: EnergyConsumer |
        EnergyBidirectional
  ): ZIO[Any, Overheat, Unit] = ???
  def drawPowerFrom(
      producer: EnergyProducer |
        EnergyBidirectional
  ): ZIO[Any, InsufficientPower, Unit] = ???

object EnergyBidirectional:
  object HomeBattery extends EnergyBidirectional:
    override val producingPriority: Int = 3
    override val consumerPriority: Int  = 2

  object ExternalGrid
      extends EnergyBidirectional:
    override val producingPriority: Int = 1
    override val consumerPriority: Int  = 1

case class Grid(
    participants: Set[EnergyParticipant]
)

trait MunicipalGrid

case class User():
  val live: ZIO[Clock, UnsatisfiedNeeds, Unit] =
    ???

case class Home(family: User, grid: Grid):
  val provide =
    for _ <- family.live
    yield ()

sealed trait GridErrors

object GridErrors:
  trait Unpowered
  trait UnsatisfiedNeeds
  trait InsufficientPower
  trait Overheat

object GridSpec extends ZIOSpecDefault:
  def spec =
    suite("GridSpec")(
      test("recognizes grid input")(
        for _ <- ZIO.unit
        yield assertNever("Need a test!")
      ),
      test("runs through an energy scenario")(
        /* We start by running our dishwasher
         * before the sun is hitting our panels,
         * so we are drawing power fully from the
         * grid. Once the panels are active, they
         * provide most of the power, but we
         * still need some from the grid. Once
         * the dishes finished, we start feeding
         * the solar power back into the grid.
         *
         * 8:00 8:30 9:00 Dishwasher -1.5kw
         * -1.5kw 0kw Solar Panels 0kw +1.0kw
         * +1.0kw CityGrid +1.5kw +0.5kw -1.0kw */
        for _ <- ZIO.unit
        yield assertNever("Need a test!")
      )
    ) @@ ignore
end GridSpec
