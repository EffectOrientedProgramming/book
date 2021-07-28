package monads

sealed trait Result [+E, +A]:
  self =>
    def flatMap[E1, B](f: A => Result[E1, B]): Result[E | E1, B] =
      self.match
        case Success(a) => f(a)
        case fail: Fail[E] => fail

    def map[B](f: A => B): Result[E, B] =
      self.match
        case Success(a) => Success(f(a))
        case fail: Fail[E] => fail

case class Fail[+E](fail: E) extends Result[E, Nothing]
case class Success[+A](succeed: A) extends Result[Nothing, A]

@main
def resultEssence =
  println("The essence of an error monad")