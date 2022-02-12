package random

import scala.util.Random

def rollDice(): Int = Random.nextInt(6) + 1

@main
def randNumEx =
  println(rollDice())
  println(rollDice())

enum GameState:
  case InProgress(roundResult: String)
  case Win
  case Lose

def scoreRound(input: Int): GameState =
  input match
    case 6 =>
      GameState.Win
    case 1 =>
      GameState.Lose
    case _ =>
      GameState.InProgress("Attempt: " + input)

def fullRound(): GameState =
  val roll = rollDice()
  scoreRound(roll)

@main
def playASingleRound() = println(fullRound())

import zio.ZIO

val rollDiceZ
    : ZIO[RandomBoundedInt, Nothing, Int] =
  RandomBoundedInt.nextIntBetween(1, 7)

import zio.{ZIO, ZIOAppDefault}
object RollTheDice extends ZIOAppDefault:
  val logic =
    for
      roll <- rollDiceZ
      _    <- ZIO.debug(roll)
    yield ()

  def run =
    logic.provideLayer(RandomBoundedInt.live)

val fullRoundZ
    : ZIO[RandomBoundedInt, Nothing, GameState] =
  for roll <- rollDiceZ
  yield scoreRound(roll)

// The problem above is that you can test the winner logic completely separate from the random number generator.
// The next example cannot be split so easily.

import zio.Ref

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
            scoreRound(roll)
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
