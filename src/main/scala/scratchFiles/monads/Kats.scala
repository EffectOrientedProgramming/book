package scratchFiles.monads.monads

import scala.util.Random

case class Box[A, V <: Dead.type | A](v: V):
  def doOnSuccess[B](f: A => Box[B, Dead.type | B]): Box[B, Dead.type | B] =
    v match
      case Dead => this.asInstanceOf[Box[B, Dead.type | B]]
      case a    => f(v.asInstanceOf[A])

object Box:
  def observe: Box[Alive.type, Dead.type | Alive.type] =
    if Random.nextBoolean() then Box(Dead) else Box(Alive)

case object Dead
case object Alive
case object Angry

def kick(alive: Alive.type): Box[Angry.type, Dead.type | Angry.type] =
  Box(Angry)

@main def kickKat =
  val kat = Box.observe.doOnSuccess(kick)
  kat.v match
    case Dead  => println("it's still dead jim")
    case Angry => println("hissssss")
