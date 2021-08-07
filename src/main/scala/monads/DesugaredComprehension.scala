package monads

val compose1 =
  for
    a <- Right("First")
    b <- Right("Second")
    c <- Right("Third")
  yield
    s"Result: $a $b $c"

val compose2 =
  Right("First")
    .flatMap(a =>
      Right("Second")
        .flatMap(b =>
          Right("Third")
            .map(c => s"Result: $a $b $c")
        )
    )

@main
  def expanded =
    println(compose1)
    println(compose2)
