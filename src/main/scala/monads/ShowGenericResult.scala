// Monads/ShowGenericResult.scala
package genericresult

def show(n: Char) =
  println(s">> show($n) <<")

  def op(id: Char) =
    val msg = s"$n|$id"
    val result =
      if n == id then
        Fail(msg)
      else
        Success(msg)
    println(s"op($id): $result")
    result
  end op

  def combine(msg: String) =
    val result =
      if n == 'd' then
        Fail(msg)
      else
        Success(msg)
    println(s"combine: $result")
    result

  val comprehension =
    for
      a <- op('a')
      b <- op('b')
      c <- op('c')
      r <- combine(s"$n|d, a:$a, b:$b, c:$c")
    yield
      println(s"Completed: $r")
      r

  comprehension match
    case Fail(msg) =>
      println(s"Failed: $msg")
    case Success(content) =>
      println(s"Succeeded: $content")
end show

@main
def results = 'a' to 'e' map show
