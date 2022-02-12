// Monads/Solution4b.scala
package monads

import ResultEnum.*

def showRE(n: Char) =
  def op(id: Char, msg: String): ResultEnum =
    val result =
      if n == id then
        FailRE(msg + id.toString)
      else
        SuccessRE(msg + id.toString)
    println(s"op($id): $result")
    result

  val compose: ResultEnum =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c.toUpperCase.nn

  println(compose)
  compose match
    case FailRE(why) =>
      println(s"Error-handling for $why")
    case SuccessRE(data) =>
      println("Success: " + data)

end showRE

@main
def resultsRE = 'a' to 'd' foreach showRE
