// Monads/ShowResult.scala
package monadresult

def show(n: Char) =
  println(s">> show $n <<")

  def op(id: Char): Result =
    val msg = s"$n|$id"
    val result =
      if n == id then
        Fail(msg)
      else
        Success(msg)
    println(s"op($id): $result")
    result

  val comprehension: Result =
    for
      a: String <- op('a')
      b: String <- op('b')
      c: String <- op('c')
    yield
      s"Completed: $n|d, a:$a, b:$b, c:$c"

  println(s"show($n): $comprehension")
end show

@main
def results = 'a' to 'd' map show
