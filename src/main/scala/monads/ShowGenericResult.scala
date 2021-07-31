// Monads/ShowGenericResult.scala
package genericresult

def show(n: Char) =
  println(s">> test $n <<")

  def op(id: Char): Result[String, String] =
    val msg = s"$n|$id"
    val result =
      if n == id then
        Fail(msg)
      else
        Success(msg)
    println(s"op($id): $result")
    result
  end op

  val comprehension =
    for
      a: String <- op('a')
      b: String <- op('b')
      c: String <- op('c')
    yield
      s"Completed: $n|d, a:$a, b:$b, c:$c"

  println(s"show($n): $comprehension")
end show

@main
def results = 'a' to 'e' map show
