// Monads/Solution1.scala
package monads

def eshow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, msg: String) =
    val result =
      if n == id then
        Left(msg + id.toString)
      else
        Right(msg + id.toString)
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

  println(compose);
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end eshow

@main
def eresults = 'a' to 'd' map eshow
