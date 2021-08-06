// Monads/Solution4a.scala
package monads

enum ResultEnum:
  def flatMap(
      f: String => ResultEnum
  ): ResultEnum =
    println(s"flatMap() on $this")
    this.match
      case SuccessRE(c) =>
        f(c)
      case fail: FailRE =>
        fail

  def map(f: String => String): ResultEnum =
    println(s"map() on $this")
    this.match
      case SuccessRE(c) =>
        SuccessRE(f(c))
      case fail: FailRE =>
        fail

  case FailRE(why: String)
  case SuccessRE(data: String)

end ResultEnum
