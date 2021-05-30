package typeclasses.game

import typeclasses.game.interactions.Interactive

trait FullInteractions[A, B]:
  extension(a: A) def interactWithAny(b: B): (A, B) | (B, A)

//
given FullInteractions[Hero, SupEnt] with
  extension(a: Hero)

    def interactWithAny(b: SupEnt): (Hero, SupEnt) | (SupEnt, Hero) =
      b match {
        case otherHero: Hero =>
          (a, otherHero)
        case monster: Monster =>
          import typeclasses.game.interactions.given_Interactive_Hero_Monster
          println("loopin'")
          a.interactWith(monster)
        case wall: Wall => (a, wall)
        case tile: Tile => (tile, a)
      }
