// Monads/GenericResult.scala
package genericresult
// TODO: Simplify the signature of Result?

trait Result[+W, +C]:
  def flatMap[W1, B](
      f: C => Result[W1, B]
  ): Result[W | W1, B] =
    println(s"flatMap() on $this")
    this.match
      case Success(c) =>
        f(c)
      case fail: Fail[W] =>
        fail

  def map[B](f: C => B): Result[W, B] =
    println(s"map() on $this")
    this.match
      case Success(c) =>
        Success(f(c))
      case fail: Fail[W] =>
        fail
end Result

case class Fail[+W](why: W)
    extends Result[W, Nothing]
case class Success[+C](content: C)
    extends Result[Nothing, C]
