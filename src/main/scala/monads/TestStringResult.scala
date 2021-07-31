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
  end op

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
      // This completely undermines the Failure
      // behavior, always giving you a Success
      // result at the end,
      // regardless of n != 'd'
      result.toString

  println(s"test($n): $test")

  def finalResult(msg: String): Result =
    if n == 'd' then
      Fail(msg)
    else
      Success(msg)

  val test2: Result =
    for
      a: String <- op('a')
      b: String <- op('b')
      c: String <- op('c')
      res <- finalResult(s"$n|d, $a, $b, $c")
    yield
      // Now we only get to the yield in the
      // success case
      println(s"yielding $res")
      res

  println(s"test($n): $test2")
end test

@main
def results = 'a' to 'e' map test
