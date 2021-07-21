package typeclasses.game

import typeclasses.game.GameTypeclasses.{
  Board,
  Cell
}
import typeclasses.game.interactions.Interactive
import typeclasses.game.rendering.Drawable
import typeclasses.game.{
  Hero,
  Monster,
  SupEnt,
  Tile
}

object GameTypeclasses:

  case class Board(
      vector: Vector[Vector[Cell[_]]]
  )

  def renderBoard(board: Board): String =
    board.vector
      .map { row =>
        row
          .map { cell =>
            cell.draw()
          }
          .mkString("")
      }
      .mkString("\n")

  case class Cell[A](a: A)(using
      drawable: Drawable[A]
  ):
    def draw() = drawable.symbol()

  trait Direction

  trait Moveable[A, B, Interactive[A, B]]:

    extension (a: A)
      def move(direction: Direction): (A, B)

  def parseCharacter(
      input: Char,
      availableDrawables: List[Drawable[_]]
  ): Drawable[_] =
    availableDrawables
      .find(_.symbol() == input.toString)
      .getOrElse(
        throw new RuntimeException(
          "could not find a Drawable for character: " + input
        )
      )

  /*
  def parseGrid(fullBoardRepresentation: String, availableDrawables: List [Drawable[_]] ): Board =
    Board(
    fullBoardRepresentation.split("\n")
      .map( row =>
        row.map{symbol =>
          // TODO Figure out how to avoid implicit
          implicit val drawable = parseCharacter(symbol, availableDrawables)
          val instance = drawable.constructor()
          Cell(instance)
        }.toVector
      ).toVector
    )

   */

  // TODO explore commutative version

  @main def gameStuff() =
    println("eh?")
    val hero = Hero()
    val monster = Monster()

    import typeclasses.game.given_FullInteractions_Hero_SupEnt
    val other: SupEnt = monster
    hero.interactWithAny(other)
    println(hero.interactWithAny(Tile()))

    val startingState =
      """
        |######
        |#@___#
        |#___M#
        |#__MM#
        |""".stripMargin

    import typeclasses.game.rendering.given

    val allDrawables =
      List(
        given_Drawable_Hero,
        given_Drawable_Monster,
        given_Drawable_Wall,
        given_Drawable_Tile
      )

//    println(renderBoard(parseGrid(startingState, allDrawables)))

    println(
      renderBoard(
        Board(
          Vector(
            Vector(
              Cell(Wall()),
              Cell(Wall()),
              Cell(Wall()),
              Cell(Wall())
            ),
            Vector(Cell(hero), Cell(Tile())),
            Vector(
              Cell(Tile()),
              Cell(monster)
            ),
            Vector(
              Cell(Wall()),
              Cell(Wall()),
              Cell(Wall()),
              Cell(Wall())
            )
          )
        )
      )
    )
