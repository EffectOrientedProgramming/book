// Monads/Result.scala
package monads

// More illumination but not more reader clarity...

// TODO: Show yield expression failing, for completeness

def test(n: Int) =
  println(s">> test $n <<")

  def produceResult(id: Int) =
    val msg = s"n:$n|id:$id"
    val result = if n == id
      then Fail(msg)
      else Succeed(msg)
    println(s"produceResult($id): $result")
    result

  val yielded = for
    i: String <- produceResult(0)
    j: String <- produceResult(1)
    k: String <- produceResult(2)
  yield (i, j, k)

  println(s"test($n) produced $yielded")

@main
def results =
  0 to 3 map test

// Shown later in the chapter:

trait Result[+E, +A]:
  def flatMap[E1, B](f: A => Result[E1, B]): Result[E | E1, B] =
    println(s"flatMap() on $this")
    val fm = this.match
      case Succeed(a) => f(a)
      case fail: Fail[E] => fail
    println(s"flatMap() on $this produces $fm")
    fm

  def map[B](f: A => B): Result[E, B] =
    println(s"map() on $this")
    val m = this.match
      case Succeed(a) => Succeed(f(a))
      case fail: Fail[E] => fail
    println(s"map() on $this produces $m")
    m

case class Fail[+E](fail: E) extends Result[E, Nothing]
case class Succeed[+A](succeed: A) extends Result[Nothing, A]

// TODO: Can Result be further simplified?