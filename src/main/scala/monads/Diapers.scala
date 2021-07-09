package monads

import scala.util.Random

enum Diaper:

  def flatMap(f: String => Diaper): Diaper =
    this match
      case _: Empty => this
      case s: Soiled =>
        f(
          s.description
        ) // written a different way for illustrating the different syntax options

  def map(f: String => String): Diaper =
    this match
      case _: Empty            => this
      case Soiled(description) => Soiled(f(description))

  // optionally we can build this on top of flatMap

  // flatMap(f.andThen(Soiled.apply))

  /*
  flatMap { description =>
    Soiled(f(description))
  }
   */

  case Empty()
  case Soiled(description: String)

def look: Diaper =
  val diaper =
    if (Random.nextBoolean())
      Diaper.Empty()
    else
      Diaper.Soiled("Ewwww")

  println(diaper)

  diaper

def change(description: String): Diaper =
  println("changing diaper")
  Diaper.Empty()

@main def baby =
  val diaper: Diaper =
    for
      soiled <-
        look // When this returns Diaper.empty, we fall out and don't get to the left side
      freshy <-
        change(
          soiled
        ) // When this returns Diaper.empty, we fall out and don't get to the left side
    yield {
      throw new RuntimeException(
        "This will never happen."
      ) // TODO Alter example so we don't have a pointless yield
    }

  println(diaper)
