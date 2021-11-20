package random

import zio.{Console, UIO, ZIO, ZLayer}
import zio.Runtime.default.unsafeRun
import fakeEnvironmentInstances.FakeConsole

val low  = 1
val high = 10

val prompt =
  s"Pick a number between $low and $high: "

// TODO Determine how to handle .toInt failure
// possibility
def checkAnswer(
    answer: Int,
    guess: String
): String =
  if answer == guess.toInt then
    "You got it!"
  else
    s"BZZ Wrong!! Answer was $answer"

val sideEffectingGuessingGame =
  for
    _ <- Console.print(prompt)
    answer = scala.util.Random.between(low, high)
    guess <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response

@main
def runSideEffectingGuessingGame =
  unsafeRun(
    sideEffectingGuessingGame.provide(
      ZLayer.succeed(FakeConsole.single("3"))
    )
  )

import zio.Console.printLine

trait RandomInt:
  def between(high: Int, low: Int): UIO[Int]

object RandomInt:
  def between(
      low: Int,
      high: Int
  ): ZIO[RandomInt, Nothing, Int] =
    // TODO Study and determine how/when to
    // introduct `serviceWith`
    ZIO.service[RandomInt].flatMap(_.between(high, low))

  object LiveRandomIntBetween extends RandomInt:

    override def between(
        high: Int,
        low: Int
    ): UIO[Int] =
      ZIO.succeed(
        scala.util.Random.between(low, high)
      )
end RandomInt

class FakeRandomInt(hardcodedValue: Int)
    extends RandomInt:

  override def between(
      high: Int,
      low: Int
  ): UIO[Int] = UIO.succeed(hardcodedValue)

val effectfulGuessingGame =
  for
    _      <- Console.print(prompt)
    answer <- RandomInt.between(low, high)
    guess  <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response

@main
def runEffectfulGuessingGame =
  unsafeRun(
    effectfulGuessingGame.provide(
      ZLayer.succeed(FakeConsole.single("3")) ++
        ZLayer
          .succeed[RandomInt](FakeRandomInt(3))
    )
  )
