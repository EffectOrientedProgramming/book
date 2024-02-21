package typeclasses.game.interactions

import typeclasses.game.{Hero, Monster}

trait Interactive[A, B]:

  extension (a: A)
    def interactWith(b: B): (A, B)

given Interactive[Hero, Monster] with

  extension (a: Hero)
    def interactWith(b: Monster) =
      println(s"$a interacted with $b")
      (a, b)

// This makes a commutative interaction
given Interactive[Monster, Hero] with

  extension (a: Monster)
    def interactWith(b: Hero) =
      val (hero, monster) =
        b.interactWith(a)
      (monster, hero)
