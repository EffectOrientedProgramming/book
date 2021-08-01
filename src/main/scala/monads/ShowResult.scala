// Monads/ShowResult.scala
package monadresult

def show(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char): Result =
    val msg = s"$n|$id"
    val result =
      if n == id then
        Fail(msg)
      else
        Success(msg)
    println(s"op($id): $result")
    result
  end op

  def combine(msg: String): Result =
    val result =
      if n == 'd' then
        Fail(msg)
      else
        Success(msg)
    println(s"combine: $result")
    result

  val comprehension: Result =
    for
      a: String <- op('a')
      b: String <- op('b')
      c: String <- op('c')
      r <- combine(s"$n|d, a:$a, b:$b, c:$c")
    yield
      println(s"Completed: $r")
      r

  println(s"result: $comprehension")
end show

@main
def results = 'a' to 'e' map show
