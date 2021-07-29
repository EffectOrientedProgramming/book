// Monads/Result.scala
package monads

// Much closer but not finished yet...

def test(n: Int) =
  println(s">> test $n <<")

  def produceResult(id: Int) =
    val result = if n == id
      then Fail(s"n:$n, id:$id")
      else Success(s"n:$n, id:$id")
    println(s"produceResult($id): $result")
    result

  // TODO: Figure out how to use intermediate results meaningfully:
  val yielded = for
    i <- produceResult(0)
    j <- produceResult(1)
    k <- produceResult(2)
  yield produceResult(3)

  println(s"test($n) produced $yielded")

@main
def resultEssence =
  0 to 4 map test

// Shown later in the chapter:

trait Result[+E, +A]:
  def flatMap[E1, B](f: A => Result[E1, B]): Result[E | E1, B] =
    println("flatMap()")
    this.match
      case Success(a) => f(a)
      case fail: Fail[E] => fail

  def map[B](f: A => B): Result[E, B] =
    println("map()")
    this.match
      case Success(a) => Success(f(a))
      case fail: Fail[E] => fail

case class Fail[+E](fail: E) extends Result[E, Nothing]
case class Success[+A](succeed: A) extends Result[Nothing, A]

// TODO: Can Result be further simplified?