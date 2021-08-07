package monads

val fc1 =
  for
    a <- Right("A")
    b <- Right("B")
    c <- Right("C")
  yield
    s"Result: $a $b $c"

val fc2 =
  Right("A")
    .flatMap(a =>
      Right("B")
        .flatMap(b =>
          Right("C")
            .map(c => s"Result: $a $b $c")
        )

@main
def expanded =
  println(compose1)
  println(compose2)
