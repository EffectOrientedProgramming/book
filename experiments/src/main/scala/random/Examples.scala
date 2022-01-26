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

val rollDiceZ
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
      roll1 <- rollDiceZ
      roll2 <- rollDiceZ
      _     <- ZIO.debug(roll1)
      _     <- ZIO.debug(roll2)
    yield ()

val fullRoundZ
    : ZIO[RandomBoundedInt, Nothing, String] =
  for
    roll1 <- rollDiceZ
    roll2 <- rollDiceZ
  yield (roll1, roll2) match
    case (6, 6) =>
      "Jackpot! Winner!"
    case (1, 1) =>
      "Snake eyes! Loser!"
    case (_, _) =>
      "Nothing interesting. Try again."

// The problem above is that you can isolate the winner logic and adequately test the program.
val fullRoundZSplit
    : ZIO[RandomBoundedInt, Nothing, String] =
  val rollBothDie =
    for
      roll1 <- rollDiceZ
      roll2 <- rollDiceZ
    yield (roll1, roll2)

  // Can be fully tested
  def checkDie(roll1: Int, roll2: Int) =
    (roll1, roll2) match
      case (6, 6) =>
        "Jackpot! Winner!"
      case (1, 1) =>
        "Snake eyes! Loser!"
      case (_, _) =>
        "Nothing interesting. Try again."

  for
    rolls <- rollBothDie
  yield checkDie(rolls._1, rolls._2)
end fullRoundZSplit

object FullRoundDecomposed

// The next example cannot be split so easily.

import zio.Ref

enum GameState:
  case InProgress(roundResult: String)
  case Win
  case Lose

val threeChances =
  for
    remainingChancesR <- Ref.make(3)

    finalGameResult <-
      (
        for
          roll <- rollDiceZ
          remainingChances <-
            remainingChancesR.getAndUpdate(_ - 1)
        yield
          if (remainingChances == 0)
            GameState.Lose
          else
            roll match
              case 6 =>
                GameState.Win
              case 1 =>
                GameState.Lose
              case _ =>
                GameState
                  .InProgress("Attempt: " + roll)
      ).repeatWhile {
        case GameState.InProgress(_) =>
          true
        case _ =>
          false
      }
    _ <-
      ZIO.debug(
        "Final game result: " + finalGameResult
      )
  yield ()

object ThreeChances extends ZIOAppDefault:
  def run =
    threeChances.provide(
      RandomBoundedIntFake.apply(Seq(2, 5, 6))
    )

object LoseInTwoChances extends ZIOAppDefault:
  def run =
    threeChances.provide(
      RandomBoundedIntFake.apply(Seq(2, 1))
    )
