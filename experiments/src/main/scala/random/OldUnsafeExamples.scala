package random

import zio.{Tag, UIO, ZEnv, ZIO, ZIOAppArgs}

import java.io.IOException
import scala.util.Random

// Anything that has a randomly generated
// component is an effect

def rollDice(): Int = Random.nextInt(6) + 1

@main
def randNumEx =
  println(rollDice())
  println(rollDice())

def fullRound(): String =
  val roll1 = rollDice()
  val roll2 = rollDice()

  (roll1, roll2) match
    case (6, 6) =>
      "Jackpot! Winner!"
    case (1, 1) =>
      "Snake eyes! Loser!"
    case (_, _) =>
      "Nothing interesting. Try again."

@main
def playUntilWinOrLoss() = println(fullRound())

def rollDiceZ()
    : ZIO[RandomBoundedInt, Nothing, Int] =
  RandomBoundedInt.nextIntBetween(1, 7)

import zio.ZIOApp
import zio.{Tag, UIO, ZEnv, ZIOAppArgs, ZLayer}

object randNumExZ extends ZIOApp:
  type Environment = RandomBoundedInt

  val layer: ZLayer[
    ZIOAppArgs,
    Any,
    RandomBoundedInt
  ] = RandomBoundedInt.live

  val tag: Tag[RandomBoundedInt] =
    Tag[RandomBoundedInt]
  def run =
    for
      roll1 <- rollDiceZ()
      roll2 <- rollDiceZ()
      _     <- ZIO.debug(roll1)
      _     <- ZIO.debug(roll2)
    yield ()

def fullRoundZ(): String =
  val roll1 = rollDice()
  val roll2 = rollDice()

  (roll1, roll2) match
    case (6, 6) =>
      "Jackpot! Winner!"
    case (1, 1) =>
      "Snake eyes! Loser!"
    case (_, _) =>
      "Nothing interesting. Try again."
