package typeclasses.game

class Hero()

class Monster()
class Teleporter()
class Food()
class Wall()
class Tile()

type ActiveEntity =
  Hero |
    Monster // TODO Worth exploring? Or should it just be the hero that is active?

type SupEnt =
  Hero | Monster | Wall | Tile

case class SupportedEntity(target: SupEnt)
