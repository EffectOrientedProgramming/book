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
    (roll1, roll2)
      match
        case (6, 6) =>
          "Jackpot! Winner!"
        case (1, 1) =>
          "Snake eyes! Loser!"
        case (_, _) =>
          "Nothing interesting. Try again."

  for
    rolls <- rollBothDie
  yield checkDie(rolls._1, rolls._2)

object FullRoundDecomposed

// The next example cannot be split so easily.

import zio.Ref

enum GameState:
  case GameInProgress(roundResult: String)
  case Win
  case Lose

val threeChances =
  for
    remainingChancesR <- Ref.make(3)
    gameResult <- Ref.make[GameState](GameState.GameInProgress("Started"))

    _ <-
      (
        for
          roll <- rollDiceZ
          _    <- ZIO.debug(roll)
          remainingChances <-
            remainingChancesR.get
          _ <- gameResult.set(
            if(roll == 6)
              GameState.GameInProgress("Winner!")
            else GameState.GameInProgress("Still playing. Last Attempt: " + roll)
          )
          _ <-
            remainingChancesR
              .set(remainingChances - 1)
        yield ()
      ).repeatWhileZIO(x =>
        for
          remainingChancesValue <- remainingChancesR.get
          gameResultValue <- gameResult.get
        yield remainingChancesValue > 0 &&  (gameResultValue match  {
          case GameState.GameInProgress(_) => true
          case _ => false
        })
      )
    finalGameResult <- gameResult.get
    _ <- ZIO.debug("Final game result: " + finalGameResult)
  yield ()

object ThreeChances extends ZIOAppDefault:
  def run =
    threeChances.provide(RandomBoundedIntFake.apply(Seq(2, 5, 6)))


object FourceChances extends ZIOAppDefault:
  def run =
    threeChances.provide(RandomBoundedIntFake.apply(Seq(2, 5, 4, 1)))