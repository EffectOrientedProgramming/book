// Monads/GenericResult.scala
package genericresultmonad

trait Result[+W, +D]:
  def flatMap[W1, B](
      f: D => Result[W1, B]
  ): Result[W | W1, B] =
    println(s"flatMap() on $this")
    this.match
      case Success(c) =>
        f(c)
      case fail: Fail[W] =>
        fail

  def map[B](f: D => B): Result[W, B] =
    println(s"map() on $this")
    this.match
      case Success(c) =>
        Success(f(c))
      case fail: Fail[W] =>
        fail
end Result

case class Fail[+W](why: W)
    extends Result[W, Nothing]
case class Success[+D](data: D)
    extends Result[Nothing, D]
