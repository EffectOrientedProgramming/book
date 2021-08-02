// Monads/Result.scala
package monadresult

trait Result:
  def flatMap(f: String => Result): Result =
    println(s"flatMap() on $this")
    this.match
      case Success(c) =>
        f(c)
      case fail: Fail =>
        fail

  def map(f: String => String): Result =
    println(s"map() on $this")
    this.match
      case Success(c) =>
        Success(f(c))
      case fail: Fail =>
        fail
end Result

case class Fail(why: String) extends Result
case class Success(data: String) extends Result
