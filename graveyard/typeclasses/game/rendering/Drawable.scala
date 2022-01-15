package typeclasses.game.rendering

import typeclasses._
import typeclasses.game._

trait Drawable[A]:
  def symbol(): String
  def constructor(): A

given Drawable[Hero] with
  def symbol(): String    = "@"
  def constructor(): Hero = Hero()

given Drawable[Monster] with
  def symbol(): String       = "M"
  def constructor(): Monster = Monster()

given Drawable[Tile] with
  def symbol(): String    = "_"
  def constructor(): Tile = Tile()

given Drawable[Wall] with
  def symbol(): String    = "#"
  def constructor(): Wall = Wall()
