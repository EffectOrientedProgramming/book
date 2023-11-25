# Random

We _need_ randomness in many domains.

- Cryptography
- Simulations
- Adding noise to images for further processing
- Adding jitter to processes to prevent clashes

Randomness might seem antithetical to functional programming.
We want pure functions that always give us the same output for the same input!

This conflict highlights that Randomness is special - it is an effect.

It is easy to miss the significance of Randomness when writing software in other languages and paradigms.
You want a random integer? Just call the `randomInt` function and move on.
It probably doesn't seem any more unusual than calling `squareRoot` or `sin`.

We use pseudorandom algorithms to produce output that is sufficiently random for some applications.
These are initialized with a seed value that determines all the following output.

```scala
class MutableRNG(var seed: Int):

  def nextInt(): Int =
    seed = mangleNumber(seed)
    seed

  private def mangleNumber(input: Int): Int =
    // *NOT* good pseudorandom logic
    input * 52357 % 1000
```

```scala
val rng = MutableRNG(1)
// rng: MutableRNG = repl.MdocSession$MdocApp$MutableRNG@23cb77df
rng.nextInt()
// res0: Int = 357
rng.nextInt()
// res1: Int = 449
rng.nextInt()
// res2: Int = 293
```
This is good enough for many situations, but is not random enough for security applications.
Let's see what happens if we make a new instance with the same seed.

```scala
val rngDuplicate = MutableRNG(1)
// rngDuplicate: MutableRNG = repl.MdocSession$MdocApp$MutableRNG@363077da
rngDuplicate.nextInt()
// res3: Int = 357
rngDuplicate.nextInt()
// res4: Int = 449
rngDuplicate.nextInt()
// res5: Int = 293
```
Exactly the same.
If an adversary is able to determine what seed is used in your application, they can predict the future to exploit your system.

## Physical RNGs
Consider a Random Number Generator (RNG) that operates by tossing a coin into the air and sending the result to the CPU.
Assuming good conditions, this is actually a good source of randomness.

Unfortunately, producing large numbers this way is slow and energy-consuming.
Optimistically you can create 1 random bit per second.
Further, you must power and maintain your coin-flipping and reading mechanism.

You can produce random data more efficiently by 

- monitoring voltage on a wire
- Reading weather sensor data
- Monitoring stock markets

You can even subscribe to services that combine all of these techniques to produce random data.

## Predictable Randomness
When your program treats randomness as an effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

```scala
import zio.test.TestRandom

TestRandom.feedBooleans(true, false)
TestRandom.feedBytes(Chunk(1, 2, 3))
TestRandom.feedChars('a', 'b', 'c')
TestRandom.feedDoubles(1.0, 2.0, 3.0)
TestRandom.feedFloats(1.0f, 2.0f, 3.0f)
TestRandom.feedInts(1, 2, 3)
TestRandom.feedLongs(1L, 2L, 3L)
TestRandom.feedStrings("a", "b", "c")
TestRandom.feedUUIDs(
  java
    .util
    .UUID
    .fromString(
      "00000000-0000-0000-0000-000000000000"
    ),
  java
    .util
    .UUID
    .fromString(
      "00000000-0000-0000-0000-000000000001"
    ),
  java
    .util
    .UUID
    .fromString(
      "00000000-0000-0000-0000-000000000002"
    )
)
```

```scala

```

If needed, you can also clear out these values by calling the various `clear` methods.

```scala
TestRandom.clearBooleans
TestRandom.clearBytes
// etc ...
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/random.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/random/DiceRolling.scala
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

val rollDiceZ: ZIO[Any, Nothing, Int] =
  zio.Random.nextIntBetween(1, 7)

import zio.{ZIO, ZIOAppDefault}
object RollTheDice extends ZIOAppDefault:
  val logic = rollDiceZ.debug

  def run = logic

val fullRoundZ: ZIO[Any, Nothing, GameState] =
  rollDiceZ.map(scoreRound)

// The problem above is that you can test the winner logic completely separate from the random number generator.
// The next example cannot be split so easily.

import zio.Ref

val threeChances =
  defer {
    val remainingChancesR = Ref.make(3).run
    val gameState =
      Ref
        .make[GameState](
          GameState.InProgress("Starting")
        )
        .run

    while (
      gameState.get.run == GameState.InProgress
    ) {
      rollDiceZ.run
      val remainingChances =
        remainingChancesR.getAndUpdate(_ - 1).run
      if (remainingChances == 0)
        gameState.set(GameState.Lose).run
    }

    val finalGameState =
      gameState
        .get
        .run // note: this has to be outside the debug parameter
    ZIO
      .debug(
        "Final game result: " + finalGameState
      )
      .run
  }

/* Rewrite these as test cases using the standard
 * zio.Random
 *
 * object ThreeChances extends ZIOAppDefault:
 * def run =
 * threeChances.provide(
 * RandomBoundedIntFake.apply(Seq(2, 5, 6)) )
 *
 * object LoseInTwoChances extends ZIOAppDefault:
 * def run =
 * threeChances.provide(
 * RandomBoundedIntFake.apply(Seq(2, 1)) ) */

```


### experiments/src/main/scala/random/RandomGuessingGame.scala
```scala
package random

val low  = 1
val high = 10

val prompt =
  s"Pick a number between $low and $high: "

// TODO Determine how to handle .toInt failure possibility
def checkAnswer(
    answer: Int,
    guess: String
): String =
  println(
    "Comparing: " + answer + " VS " + guess.toInt
  )
  if answer == guess.toInt then
    "You got it!"
  else
    s"BZZ Wrong!! Answer was $answer"

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
        s"BZZ Wrong!!"
    )
    .merge

val sideEffectingGuessingGame =
  defer:
    Console.print(prompt).run
    val answer =
      scala.util.Random.between(low, high)
    println("Side effecting random #: " + answer)
    val guess = Console.readLine.run
    checkAnswer(answer, guess)

val effectfulGuessingGame =
  defer {
    Console.print(prompt).run
    val answer =
      Random.nextIntBetween(low, high).run
    val guess = Console.readLine.run
    checkAnswerZSplit(answer, guess).run
  }

```

