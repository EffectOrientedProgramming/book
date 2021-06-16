package monads.adts

import scala.util.Random

case object Box
case object Dead
case object Alive

def observe(box: Box.type): Dead.type | Alive.type =
  if (Random.nextBoolean()) Dead else Alive

@main def checkKatHealth =
  val kat = observe(Box)
  kat match
    case Dead  => println("it's dead jim")
    case Alive => println("it's bouncing off the walls")

case object Angry

def kick(kat: Dead.type | Alive.type): Dead.type | Angry.type =
  kat match
    case Dead  => Dead
    case Alive => Angry

@main def kickKat =
  val kat = kick(observe(Box))
  kat match
    case Dead  => println("it's still dead jim")
    case Angry => println("hissssss")
