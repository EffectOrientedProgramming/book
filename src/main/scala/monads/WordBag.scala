package monads

enum WordBag:

  def flatMap(f: String => WordBag): WordBag =
    this match
      case _: Empty => this
      case s: Full =>
        f(
          s.content
        ) // written a different way for illustrating the different syntax options

  def map(f: String => String): WordBag =
    this match
      case _: Empty      => this
      case Full(content) => Full(f(content))
  case Empty()
  case Full(content: String)

def httpOperation(content: String): WordBag =
  if (content == "DB Operation")
    WordBag.Full(content + " + HTTP operation")
  else
    WordBag.Empty()

@main def wordOperations =
  val result =
    for
      firstBagContent <- WordBag.Full("DB Operation")
      step2content <- httpOperation(firstBagContent)
      step3content <- WordBag.Full(step2content + " + Dictionary lookup")
    yield step3content

  println(result)
