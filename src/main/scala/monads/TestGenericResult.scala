// Monads/TestGenericResult.scala
package genericresult

def test(n: Char) =
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

  val test =
    for
      a: String <- op('a')
      b: String <- op('b')
      c: String <- op('c')
    yield
      val msg = s"$n|d, $a, $b, $c"
      val result =
        if n == 'd' then
          Fail(msg)
        else
          Success(msg)
      println(s"yielding $result")
      result

  println(s"test($n): $test")
end test

@main
def results = 'a' to 'e' map test
