package energrygrid

import zio._
import zio.test._
import energrygrid.GridErrors._

/*
  TODO Simulate a home-level grid managing power needs and production
    Why?
      - This is code that interacts with TheWorld in numerous ways
      - It's something I've wanted a more visceral understanding of
*/
trait EnergyParticipant

enum EnergyProducer extends EnergyParticipant:
  case SolarPanels, Generator
  def sendPowerTo(consumer: EnergyConsumer | EnergyBidirectional): ZIO[Any, Overheat, Unit] = ???

enum EnergyConsumer extends EnergyParticipant:
  case Dishwasher, WiFi, Refrigerator, Lights
  def drawPowerFrom(producer: EnergyProducer | EnergyBidirectional): ZIO[Any, InsufficientPower, Unit] = ???

enum EnergyBidirectional extends EnergyParticipant:
  case HomeBattery, ElectricVehicle, ExternalGrid
  def sendPowerTo(consumer: EnergyConsumer | EnergyBidirectional): ZIO[Any, Overheat, Unit] = ???
  def drawPowerFrom(producer: EnergyProducer | EnergyBidirectional): ZIO[Any, InsufficientPower, Unit] = ???

case class Grid(
  participants: Set[EnergyParticipant]
)

trait MunicipalGrid

case class User():
  val live: ZIO[Clock, UnsatisfiedNeeds, Unit] = ???

case class Home(
                 family: User,
                 grid: Grid
               ):
  val provide =
    for {
      _ <- family.live
    } yield ()

sealed trait GridErrors

object GridErrors:
  trait Unpowered
  trait UnsatisfiedNeeds
  trait InsufficientPower
  trait Overheat


object GridSpec extends ZIOSpecDefault {
  def spec =
    test("recognizes grid input")(
      for
        _ <- ZIO.unit
      yield assertNever("Need a test!")
    )

}
