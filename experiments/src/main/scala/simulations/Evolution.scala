package simulations

import zio._

enum Action:
  case Stay
  case Move(x: Int, y: Int)

object Evolution extends ZIOAppDefault:
  case class Percentage(value: Int):
    assert(value >= 0 & value <= 100)
  case class Creature(
      energy: Int,
      explore: Percentage
  )

  case class Coordinate(
      food: Int,
      produceFood: Percentage,
      occupant: Option[Creature]
  )
  case class Spots(
      coordinates: List[List[Coordinate]]
  ):
    override def toString() =
      coordinates
        .map(row =>
          row
            .map(column =>
              if (column.food > 0)
                '*'
              else
                '\u25a1'
            )
            .mkString
        )
        .mkString("\n")

  object Spots:
    def apply(rows: Int, columns: Int): Spots =
      Spots(
        List.fill(rows)(
          List.fill(columns)(
            Coordinate(
              1,
              Percentage(10),
              occupant = None
            )
          )
        )
      )

  def run =
    for
      _ <- ZIO.log("Should do evolution stuff")
      spots = Spots(4, 4)
      _ <- ZIO.debug(spots)
    yield ()
end Evolution

object EvolutionT:
  trait Creature:
    def decide(): Action

  trait Spots:
    def udpate(): Spots
