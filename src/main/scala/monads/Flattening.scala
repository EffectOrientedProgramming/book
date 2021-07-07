package monads

@main def hmmm() =

  val abc = List("a", "bb", "ccc")
  println(abc.flatten)
  val abbccc = abc.flatMap { s =>
    s.toCharArray match {
      case a: Array[Char] => a.toList
      case _: Null => List.empty
    }
  }
  println(abbccc)

  // We need to provide an instance of a function that can
  // transform an Int to a IterableOnce[B]
  implicit def intToIterable(i: Int): IterableOnce[Int] =
    List.fill(i)(i)

  val oneAndFiveFives = List(1, 5)
  println(oneAndFiveFives.flatten)