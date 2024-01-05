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
  parse:
    guess
  .map(i =>
    if answer == i then
      "You got it!"
    else
      s"BZZ Wrong!!"
  )
  .merge

val effectfulGuessingGame =
  defer:
    Console.print:
      prompt
    .run
    val answer =
      Random.nextIntBetween(low, high).run
    val guess = Console.readLine.run
    checkAnswerZSplit(answer, guess).run
