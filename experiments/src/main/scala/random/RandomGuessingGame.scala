package random

import console.FakeConsole
import zio._

val low = 1
val high = 10

val prompt =
  s"Pick a number between $low and $high: "

// TODO Determine how to handle .toInt failure possibility
def checkAnswer( answer: Int, guess: String ): String =
  if answer == guess.toInt then "You got it!"
  else s"BZZ Wrong!! Answer was $answer"

val sideEffectingGuessingGame =
  for
    _ <- Console.print(prompt)
    answer = scala.util.Random.between(low, high)
    guess <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response

object runSideEffectingGuessingGame extends ZIOAppDefault:

  def run =
    sideEffectingGuessingGame
      .debug("Side effecting results")
      .provideLayer(
      ZLayer.succeed(FakeConsole.single("3")) )

val effectfulGuessingGame =
  for
    _ <- Console.print(prompt)
    answer <- RandomBoundedInt.nextIntBetween(low, high)
    guess <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response

object RunEffectfulGuessingGame extends ZIOAppDefault:
  def run =
    effectfulGuessingGame.provideLayer(
      ZLayer.succeed(
        FakeConsole.single("3")) ++ RandomBoundedInt.live
    )
