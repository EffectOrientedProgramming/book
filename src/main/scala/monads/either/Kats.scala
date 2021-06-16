package monads.either

import scala.util.Random

case object Box
case object Dead
case object Alive

def observe(box: Box.type): Either[Dead.type, Alive.type] =
  val r = Either.cond(Random.nextBoolean(), Alive, Dead)
  println("observe" -> r)
  r

@main def checkKatHealth =
  val kat = observe(Box)
  println(kat)

case object Angry

def kick(kat: Alive.type): Either[Dead.type, Angry.type] =
  val r = Either.cond(Random.nextBoolean(), Angry, Dead)
  println("kick" -> r)
  r

@main def kickKat =
  //val kat = kick(observe(Box)) // this doesn't work because you shouldn't kick a dead kat
  //val kat: Either[Dead.type, Angry.type] = observe(Box).flatMap(kick)
  val kat =
    for
      alive <- observe(Box)
      angry <- kick(alive)
    yield angry

  println(kat)

case object Fed

def feed(kat: Alive.type): Fed.type =
  Fed

@main def feedKat =
  val kat = observe(Box).map(feed)

  println(kat)
