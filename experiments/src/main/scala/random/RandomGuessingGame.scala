package random

import zio.{Console, UIO, Unsafe, ZIO, ZLayer}
import console.FakeConsole
import zio.Runtime.default.unsafe

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
  Unsafe.unsafeCompat { implicit u =>
    unsafe
      .run(
        sideEffectingGuessingGame.provideLayer(
          ZLayer.succeed(FakeConsole.single("3"))
        )
      )
      .getOrThrowFiberFailure()
  }

import zio.Console.printLine

val effectfulGuessingGame =
  for
    _ <- Console.print(prompt)
    answer <-
      RandomBoundedInt.nextIntBetween(low, high)
    guess <- Console.readLine
    response = checkAnswer(answer, guess)
  yield prompt + guess + "\n" + response

@main
def runEffectfulGuessingGame =
  Unsafe.unsafeCompat { implicit u =>
    unsafe
      .run(
        effectfulGuessingGame.provideLayer(
          ZLayer
            .succeed(FakeConsole.single("3")) ++
            RandomBoundedInt.live
        )
      )
      .getOrThrowFiberFailure()
  }
