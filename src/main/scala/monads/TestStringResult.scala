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
      val msg = s"$n|d, $a, $b, $c"
      val result: Result =
        if n == 'd' then
          Fail(msg)
        else
          Success(msg)
      println(s"yielding $result")
      result.toString

  println(s"test($n): $test")
end test

@main
def results = 'a' to 'e' map test
