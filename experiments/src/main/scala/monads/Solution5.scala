package monads

val sol5a =
  for
    a <- Some("A")
    b <- Some("B")
    c <- Some("C")
  yield s"Result: $a $b $c"

val sol5b =
  Some("A").flatMap(a =>
    Some("B").flatMap(b =>
      Some("C").map(c => s"Result: $a $b $c")
    )
  )

@main
def sol5 =
  println(sol5a)
  println(sol5b)
