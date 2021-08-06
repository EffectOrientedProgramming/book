// Monads/Solution2.scala
package monads

def ishow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, i: Int) =
    val result =
      if n == id then
        Left(i + id)
      else
        Right(i + id)
    println(s"op($id): $result")
    result
  end op

  val compose =
    for
      a: Int <- op('a', 0)
      b: Int <- op('b', a)
      c: Int <- op('c', b)
    yield
      println(s"Completed: $c")
      c

  println(compose);
  for (failure <- compose.left)
    println(s"Error-handling for $failure")

end ishow

@main
def iresults = 'a' to 'd' foreach ishow
