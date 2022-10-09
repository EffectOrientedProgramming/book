package energrygrid

import zio._
import zio.test._

trait EnergySource
trait EnergySink

case class Grid(
  sources: Set[EnergySource],
  sinks: Set[EnergySink]
)

case class Appliance() extends EnergySink:
  val run: ZIO[Grid, errors.Unpowered, Unit] = ???

case class User():
  val live: ZIO[Clock, errors.UnsatisfiedNeeds, Unit] = ???

case class Home(
                 family: User,
                 grid: Grid
               ):
  val provide =
    for {
      _ <- family.live
    } yield ()

package errors:
  trait Unpowered
  trait UnsatisfiedNeeds


object GridSpec extends ZIOSpecDefault {
  def spec =
    test("recognizes grid input")(
      for
        _ <- ZIO.unit
      yield assertNever("Need a test!")
    )

}
