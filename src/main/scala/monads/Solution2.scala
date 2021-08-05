// Monads/Solution2.scala
package monads

def oshow(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char, msg: String) =
    val result =
      if n == id then
        None
      else
        Some(msg + id.toString)
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
  if compose == None then
    println(s"Error-handling for None")

end oshow

@main
def oresults = 'a' to 'd' foreach oshow
