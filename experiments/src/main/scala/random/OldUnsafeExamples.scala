package random

import zio.{UIO, ZIO}

import scala.util.Random

object OldUnsafeExamples:

  // Anything that has a randomly generated
  // component is an effect

  def rollDice: Int =
    Random.nextInt(6) + 1

  def fullRound(): String =
    (rollDice, rollDice) match {
      case (6, 6) => "Jackpot! Winner!"
      case (1, 1) => "Snake eyes! Loser!"
      case (_, _) =>
        "Nothing interesting. Try again."
    }

  @main
  def randNumEx =
    println(rollDice)
    println(rollDice)

  @main
  def playUntilWinOrLoss() =
    println(fullRound())
// These have the same input, yet different
// outputs.

trait RandomBoundedInt:
  def nextIntBetween(
                      minInclusive: Int,
                      maxExclusive: Int
                    ): UIO[Int] =
    ZIO.succeed(Random.between(1, 7))

