// Monads/Result.scala
package monads

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
    val r =
      this.match
        case Success(c) =>
          Success(f(c))
        case fail: Fail =>
          fail
    println(s"map() returns $r")
    r

end Result

case class Fail(why: String)     extends Result
case class Success(data: String) extends Result
