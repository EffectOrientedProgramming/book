// Monads/ShowResult.scala
package monads

def show(n: Char) =
  def op(id: Char, msg: String): Result =
    val result =
      if n == id then
        Fail(msg + id.toString)
      else
        Success(msg + id.toString)
    println(s"$n => op($id): $result")
    result

  val compose: Result =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Yielding: $c + 'd'")
      c + 'd'

  println(s"compose: $compose")
  compose match
    case Fail(why) =>
      println(s"Error-handling for $why")
    case Success(data) =>
      println("Successful case: " + data)

end show

@main
def results = 'a' to 'd' foreach show
