// Monads/TestStringResult.scala
package stringresult

def test(n: Char) =
  println(s">> test $n <<")

  def op(id: Char): Result =
    val msg = s"$n|$id"
    val result =
      if n == id then
        Fail(msg)
      else
        Success(msg)
    println(s"op($id): $result")
    result

  val test: Result =
    for
      a: String <- op('a')
      b: String <- op('b')
      c: String <- op('c')
    yield
      s"Completed yield: $n|d, a:$a, b:$b, c:$c"

  println(s"test($n): $test")
end test

@main
def results = 'a' to 'd' map test
