# Random

There 

{{Subject Dependencies: `Console`, `ZIO.serviceWith`}}

TODO All the prose to justify these hoops

NOTE Moved code to `experiments/src/main/scala/random` due to dependency on code not in Chapters


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/random/Examples.scala
```scala
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
import zio.direct.*

val rollDiceZ
    : ZIO[RandomBoundedInt, Nothing, Int] =
  RandomBoundedInt.nextIntBetween(1, 7)

import zio.{ZIO, ZIOAppDefault}
object RollTheDice extends ZIOAppDefault:
  val logic = rollDiceZ.debug

  def run =
    logic.provideLayer(RandomBoundedInt.live)

val fullRoundZ
    : ZIO[RandomBoundedInt, Nothing, GameState] =
  rollDiceZ.map(scoreRound)

// The problem above is that you can test the winner logic completely separate from the random number generator.
// The next example cannot be split so easily.

import zio.Ref

val threeChances =
  defer {
    val remainingChancesR = Ref.make(3).run
    val gameState = Ref.make[GameState](GameState.InProgress("Starting")).run

    while (gameState.get.run == GameState.InProgress) {
      val roll = rollDiceZ.run
      val remainingChances = remainingChancesR.getAndUpdate(_ - 1).run
      if (remainingChances == 0)
        gameState.set(GameState.Lose).run
      else
        scoreRound(roll)
    }

    val finalGameState = gameState.get.run // note: this has to be outside the debug parameter
    ZIO.debug(
      "Final game result: " + finalGameState
    ).run
  }

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

```


### experiments/src/main/scala/random/RandomBoundedInt.scala
```scala
package random

import zio.{Tag, UIO, ZIO, ZIOAppArgs}
import scala.util.Random

trait RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int]

import zio.{UIO, ZIO, ZLayer}

import scala.util.Random

object RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): ZIO[RandomBoundedInt, Nothing, Int] =
    ZIO.serviceWithZIO[RandomBoundedInt](
      _.nextIntBetween(
        minInclusive,
        maxExclusive
      )
    )

  object RandomBoundedIntLive
      extends RandomBoundedInt:
    override def nextIntBetween(
        minInclusive: Int,
        maxExclusive: Int
    ): UIO[Int] =
      ZIO.succeed(
        Random
          .between(minInclusive, maxExclusive)
      )

  val live
      : ZLayer[Any, Nothing, RandomBoundedInt] =
    ZLayer.succeed(RandomBoundedIntLive)
end RandomBoundedInt

```


### experiments/src/main/scala/random/RandomBoundedIntFake.scala
```scala
package random

import zio.{Ref, UIO, ZIO, ZLayer}

class RandomBoundedIntFake private (
    values: Ref[Seq[Int]]
) extends RandomBoundedInt:
  def nextIntBetween(
      minInclusive: Int,
      maxExclusive: Int
  ): UIO[Int] =
    for
      remainingValues <- values.get
      nextValue <-
        if (remainingValues.isEmpty)
          ZIO.die(
            new Exception(
              "Did not provide enough values!"
            )
          )
        else
          ZIO.succeed(remainingValues.head)
      _ <- values.set(remainingValues.tail)
    yield remainingValues.head
end RandomBoundedIntFake

object RandomBoundedIntFake:
  def apply(
      values: Seq[Int]
  ): ZLayer[Any, Nothing, RandomBoundedInt] =
    ZLayer.fromZIO(
      for valuesR <- Ref.make(values)
      yield new RandomBoundedIntFake(valuesR)
    )

```


### experiments/src/main/scala/random/RandomGuessingGame.scala
```scala
package random

import console.FakeConsole
import zio._
import zio.direct.*

val low  = 1
val high = 10

val prompt =
  s"Pick a number between $low and $high: "

// TODO Determine how to handle .toInt failure possibility
def checkAnswer(
    answer: Int,
    guess: String
): String =
  if answer == guess.toInt then
    "You got it!"
  else
    s"BZZ Wrong!! Answer was $answer"

def checkAnswerTry(
    answer: Int,
    guess: String
): String =
  try
    if answer == guess.toInt then
      "You got it!"
    else
      s"BZZ Wrong!! Answer was $answer"
  catch
    case _ =>
      "User did not provide an integer. Invalid input: " +
        guess

def checkAnswerZ1(
    answer: Int,
    guess: String
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(guess.toInt)
    .map(i =>
      if answer == i then
        "You got it!"
      else
        s"BZZ Wrong!! Answer was $answer"
    )
    .catchAll(_ =>
      ZIO.succeed("Invalid input: " + guess)
    )

def checkAnswerZ2(
    answer: Int,
    guess: String
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(guess.toInt)
    .map(i =>
      if answer == i then
        "You got it!"
      else
        s"BZZ Wrong!! Answer was $answer"
    )
    .orElseSucceed("Invalid input: " + guess)

def checkAnswerZ3(
    answer: Int,
    guess: String
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(guess.toInt)
    .orElseFail("Invalid input:  " + guess)
    .map(i =>
      if answer == i then
        "You got it!"
      else
        s"BZZ Wrong!! Answer was $answer"
    )
    .merge

def checkAnswerZ4(
    answer: Int,
    guess: String
): ZIO[Any, Nothing, String] =
  ZIO
    .attempt(guess.toInt)
    .mapBoth(
      _ => "Invalid input:  " + guess,
      i =>
        if answer == i then
          "You got it!"
        else
          s"BZZ Wrong!! Answer was $answer"
    )
    .merge

// After writing so many variations of the function above,
// I suspect that what we _really_ need to do is break up the logic.
// Both ZIO and vanilla Scala code should get simpler

def parse(guess: String) =
  ZIO
    .attempt(guess.toInt)
    .orElseFail("Invalid input:  " + guess)

def checkAnswerZSplit(
    answer: Int,
    guess: String
): ZIO[Any, Nothing, String] =
  parse(guess)
    .map(i =>
      if answer == i then
        "You got it!"
      else
        s"BZZ Wrong!! Answer was $answer"
    )
    .merge

val sideEffectingGuessingGame =
  defer {
    Console.print(prompt).run
    val answer = scala.util.Random.between(low, high)
    val guess = Console.readLine.run
    val response = checkAnswer(answer, guess)
    prompt + guess + "\n" + response
  }

object runSideEffectingGuessingGame
    extends ZIOAppDefault:
  def run =
    sideEffectingGuessingGame
      .withConsole(FakeConsole.single("3"))
      .debug("Side effecting results")

val effectfulGuessingGame =
  defer {
    Console.print(prompt).run
    val answer =
      RandomBoundedInt.nextIntBetween(low, high).run
    val guess = Console.readLine.run
    val response = checkAnswerZSplit(answer, guess).run
    prompt + guess + "\n" + response
  }

// TODO Decide if these should be removed, since test cases exist now
object RunEffectfulGuessingGame
    extends ZIOAppDefault:
  def run =
    effectfulGuessingGame
      .withConsole(FakeConsole.single("3"))
      .provideLayer(RandomBoundedInt.live)

object RunEffectfulGuessingGameTestable
    extends ZIOAppDefault:
  def run =
    effectfulGuessingGame
      .debug("Result")
      .withConsole(FakeConsole.single("3"))
      .provideLayer(RandomBoundedIntFake(Seq(3)))

```


### experiments/src/main/scala/random/RandomZIOFake.scala
```scala
package random

import zio.{
  BuildFrom,
  Chunk,
  Console,
  Random,
  UIO,
  ZIO,
  ZLayer,
  Trace
}
import zio.Console.printLine

import java.util.UUID

class RandomZIOFake(i: Int) extends Random:
  def nextUUID(implicit
      trace: Trace
  ): UIO[UUID] = ???
  def nextBoolean(implicit
      trace: zio.Trace
  ): zio.UIO[Boolean] = ???
  def nextBytes(length: => Int)(implicit
      trace: zio.Trace
  ): zio.UIO[zio.Chunk[Byte]] = ???
  def nextDouble(implicit
      trace: zio.Trace
  ): zio.UIO[Double] = ???
  def nextDoubleBetween(
      minInclusive: => Double,
      maxExclusive: => Double
  )(implicit trace: zio.Trace): zio.UIO[Double] =
    ???
  def nextFloat(implicit
      trace: zio.Trace
  ): zio.UIO[Float] = ???
  def nextFloatBetween(
      minInclusive: => Float,
      maxExclusive: => Float
  )(implicit trace: zio.Trace): zio.UIO[Float] =
    ???
  def nextGaussian(implicit
      trace: zio.Trace
  ): zio.UIO[Double] = ???
  def nextInt(implicit
      trace: zio.Trace
  ): zio.UIO[Int] = ???
  def nextIntBetween(
      minInclusive: => Int,
      maxExclusive: => Int
  )(implicit trace: zio.Trace): zio.UIO[Int] =
    ???
  def nextIntBounded(n: => Int)(implicit
      trace: zio.Trace
  ): zio.UIO[Int] = ???
  def nextLong(implicit
      trace: zio.Trace
  ): zio.UIO[Long] = ???
  def nextLongBetween(
      minInclusive: => Long,
      maxExclusive: => Long
  )(implicit trace: zio.Trace): zio.UIO[Long] =
    ???
  def nextLongBounded(n: => Long)(implicit
      trace: zio.Trace
  ): zio.UIO[Long] = ???
  def nextPrintableChar(implicit
      trace: zio.Trace
  ): zio.UIO[Char] = ???
  def nextString(length: => Int)(implicit
      trace: zio.Trace
  ): zio.UIO[String] = ???
  def setSeed(seed: => Long)(implicit
      trace: zio.Trace
  ): zio.UIO[Unit] = ???
  def shuffle[A, Collection[+Element]
    <: Iterable[Element]](
      collection: => Collection[A]
  )(implicit
      bf: BuildFrom[Collection[A], A, Collection[
        A
      ]],
      trace: Trace
  ): UIO[Collection[A]] = ???

end RandomZIOFake

```

