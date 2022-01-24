package random

import zio.{
  Tag,
  UIO,
  ZEnv,
  ZIO,
  ZIOAppArgs,
  ZIOAppDefault
}

import java.io.IOException
import scala.util.Random

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

  val layer = RandomBoundedInt.live

  val tag: Tag[RandomBoundedInt] =
    Tag[RandomBoundedInt]
  def run =
    for
      roll1 <- rollDiceZ()
      roll2 <- rollDiceZ()
      _     <- ZIO.debug(roll1)
      _     <- ZIO.debug(roll2)
    yield ()

val fullRoundZ
    : ZIO[RandomBoundedInt, Nothing, String] =
  val roll1 = rollDice()
  val roll2 = rollDice()

  for
    roll1 <- rollDiceZ()
    roll2 <- rollDiceZ()
  yield (roll1, roll2) match
    case (6, 6) =>
      "Jackpot! Winner!"
    case (1, 1) =>
      "Snake eyes! Loser!"
    case (_, _) =>
      "Nothing interesting. Try again."

// The problem above is that you can isolate the winner logic and adequately test the program. The next example is not so simple

import zio.Ref

case class GameState()

val threeChances =
  for
    remainingChancesR <- Ref.make(3)
    _ <-
      (
        for
          roll <- rollDiceZ()
          _    <- ZIO.debug(roll)
          remainingChances <-
            remainingChancesR.get
          _ <-
            remainingChancesR
              .set(remainingChances - 1)
        yield ()
      ).repeatWhileZIO(x =>
        remainingChancesR.get.map(_ > 0)
      )
  yield ()

object ThreeChances extends ZIOAppDefault:
  def run =
    threeChances.provide(RandomBoundedInt.live)
