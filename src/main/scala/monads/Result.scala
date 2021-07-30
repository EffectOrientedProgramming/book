// Monads/Result.scala
package monads

def test(n: Char) =
  def op(id: Char): Result[String, String] =
    val msg = s"$n|$id"
    val result = if n == id
      then Fail(msg)
      else Success(msg)
    println(s"op($id): $result")
    result

  println(s">> test $n <<")
  val test = for
    a: String <- op('a')
    b: String <- op('b')
    c: String <- op('c')
  yield {
    val msg = s"$n|d, $a, $b, $c"
    val result = if n == 'd'
      then Fail(msg)
      else Success(msg)
    println(s"yielding $result")
    result
  }
  println(s"test($n): $test")

@main
def results =
  'a' to 'e' map test

// Shown later in the chapter:

trait Result[+E, +A]:
  def flatMap[E1, B](f: A => Result[E1, B]): Result[E | E1, B] =
    println(s"flatMap() on $this")
    this.match
      case Success(a) => f(a)
      case fail: Fail[E] => fail

  def map[B](f: A => B): Result[E, B] =
    println(s"map() on $this")
    this.match
      case Success(a) => Success(f(a))
      case fail: Fail[E] => fail

case class Fail[+E](fail: E) extends Result[E, Nothing]
case class Success[+A](succeed: A) extends Result[Nothing, A]

// TODO: Can the signature of Result be further simplified?