// Monads/Result.scala
package monads

// More illumination but not more reader clarity...

def test(n: Char) =
  println(s">> test $n <<")

  def makeResult(id: Char): Result[String, String] =
    val msg = s"$n|$id"
    val result = if n == id
      then Fail(msg)
      else Succeed(msg)
    println(s"makeResult($id): $result")
    result

  def makeYield(id: Char, i: String, j: String, k: String): Result[String, String] =
    val msg = s"$n|$id, $i, $j $k"
    val result = if n == id
      then Fail(msg)
      else Succeed(msg)
    println(s"makeYield($id): $result")
    result

  val yielded = for
    i: String <- makeResult('a')
    j: String <- makeResult('b')
    k: String <- makeResult('c')
  yield makeYield('d', i, j, k)

  println(s"test($n) produced $yielded")

@main
def results =
  'a' to 'e' map test

// Shown later in the chapter:

trait Result[+E, +A]:
  def flatMap[E1, B](f: A => Result[E1, B]): Result[E | E1, B] =
    println(s"flatMap() on $this")
    val fm = this.match
      case Succeed(a) => f(a)
      case fail: Fail[E] => fail
    println(s"flatMap() on $this: $fm")
    fm

  def map[B](f: A => B): Result[E, B] =
    println(s"map() on $this")
    val m = this.match
      case Succeed(a) => Succeed(f(a))
      case fail: Fail[E] => fail
    println(s"map() on $this: $m")
    m

case class Fail[+E](fail: E) extends Result[E, Nothing]
case class Succeed[+A](succeed: A) extends Result[Nothing, A]

// TODO: Can Result be further simplified?