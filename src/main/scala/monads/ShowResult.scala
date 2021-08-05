// Monads/ShowResult.scala
package monads

def show(n: Char) =
  def op(id: Char, msg: String): Result =
    val result =
      if n == id then
        Fail(msg + id.toString)
      else
        Success(msg + id.toString)
    println(s"op($id): $result")
    result
  end op

  val compose: Result =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c.toUpperCase.nn

  println(compose)
  compose match
    case Fail(why) =>
      println(s"Error-handling for $why")
    case Success(data) =>
      println("Success: " + data)

end show

@main
def results = 'a' to 'd' foreach show
