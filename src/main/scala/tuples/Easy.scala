package tuples

@main
def easy() =
  val six_1 =
    List(1, 2, 3).fold(0) { (total, i) =>
      total + i
    }
  assert(six_1 == 6)

  val six_2 = List(1, 2, 3).fold(0)(_ + _)
  assert(six_2 == 6)

  val addIt_1: (Int, Int) => Int =
    (total, i) => total + i
  val six_3 = List(1, 2, 3).fold(0)(addIt_1)
  assert(six_3 == 6)

  def addIt_2(total: Int, i: Int): Int =
    total + i
  val six_4 = List(1, 2, 3).fold(0)(addIt_2)
  assert(six_4 == 6)

  // does not work
  // def addIt_3(t: (Int, Int)): Int = t._1 +
  // t._2
  // val six_5 = List(1, 2, 4).fold(0)(addIt_3)
  // assert(six_5 == 6)

  val t2: (String, Int) = "asdf" -> 1

  val t3: (String, Int, Boolean) =
    ("asdf", 1, false)

  val t4: ((String, Int), Boolean) =
    "asdf" -> 1 -> false

  val m1: Map[String, Int] = Map("asdf" -> 1)

  val m2 =
    m1.map { (s, i) =>
      s.take(i)
    }
  assert(m2.head == "a")

  val m3 =
    List(1, 2, 3).map(i => i.toString -> i).toMap
  assert(m3.head == "1" -> 1)
end easy
