package monads.monads

import scala.util.Random

case object Box
sealed trait Kat:
  def doOnSuccess(f: Kat => Kat): Kat =
    this match
      case Alive => f(this)
      case _ => this

case object Dead extends Kat
case object Alive extends Kat
case object Angry extends Kat

def observe(box: Box.type): Kat =
  if (Random.nextBoolean()) Dead else Alive

// it is weird that we take a Kat here. We'd like to only take an Alive Kat but
// we don't have type parameters to guide our doOnSuccess functor
def kick(kat: Kat): Kat =
  Angry

@main def kickKat =
  val kat = observe(Box).doOnSuccess(kick)
  kat match
    case Dead => println("it's still dead jim")
    case Alive => println("oh hi there")
    case Angry => println("hissssss")