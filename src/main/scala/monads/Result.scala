// Monads/Result.scala
package monads

trait Result:
  def flatMap(f: String => Result): Result =
    println(s"flatMap() on $this")
    this.match
      case fail: Fail =>
        fail
      case Success(c) =>
        f(c)

  def map(f: String => String): Result =
    println(s"map() on $this")
    this.match
      case fail: Fail =>
        fail
      case Success(c) =>
        Success(f(c))

end Result

case class Fail(why: String)     extends Result
case class Success(data: String) extends Result
