// Monads/GenericResult.scala
package monads

trait GResult[+W, +D]:
  def flatMap[W1, B](
      f: D => GResult[W1, B]
  ): GResult[W | W1, B] =
    println(s"flatMap() on $this")
    this.match
      case GSuccess(c) =>
        f(c)
      case fail: GFail[W] =>
        fail

  def map[B](f: D => B): GResult[W, B] =
    println(s"map() on $this")
    this.match
      case GSuccess(c) =>
        GSuccess(f(c))
      case fail: GFail[W] =>
        fail

case class GFail[+W](why: W)
    extends GResult[W, Nothing]
case class GSuccess[+D](data: D)
    extends GResult[Nothing, D]
