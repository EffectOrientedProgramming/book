// Monads/ShowGenericResult.scala
// Exercise solution to "Verify
// GenericResult.scala works"
package monads

def gshow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, msg: String) =
    val result =
      if n == id then
        GFail(msg + id.toString)
      else
        GSuccess(msg + id.toString)
    println(s"op($id): $result")
    result
  end op

  val compose =
    for
      a: String <- op('a', "")
      b: String <- op('b', a)
      c: String <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  if compose.isInstanceOf[GFail[String]] then
    println(s"Error-handling for $compose")
end gshow

@main
def gresults = 'a' to 'd' map gshow
