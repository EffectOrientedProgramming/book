package tuples

@main
def fun() =
  // special type, not ()
  val t0: Unit = ()

  // single value tuples are really just the
  // value, no reason to treat it as a tuple
  // Q: is there a way to define single element
  // tuples?
  val t1: String = "asdf"
  assert(t1 == "asdf")

  // doesn't work because t1 isn't really a tuple
  // val t1_1 = 1 *: t1

  val t2: (String, Int) = ("asdf", 1)
  assert(t2._1 == "asdf")

  // bug note, the type annotation is required
  val t2_1: (Int, String, Int) = 1 *: t2
  assert(t2_1._2 == "asdf")

  // does not work with t1 cause it's not a tuple
  // val t2_t1 = t2 ++ t1

  // if we don't specify the type it is ???
  // val t2_t2 = t2 ++ t2
  val t2_t2: (String, Int, String, Int) =
    t2 ++ t2
  assert(t2_t2._3 == "asdf")

  val t2_0 = t2.drop(1)
  // val t2_0: Int *:
  // scala.Tuple$package.EmptyTuple.type =
  // t2.drop(1)
  // assert(t2_0._1 == "asdf")

  assert(t2.head == "asdf")

  assert(
    t2.toList.map(_.toString) ==
      List("asdf", "1")
  )
end fun

// computer says no
// val t2_m = t2.map(identity)
// println(t2_m)

// assert(t2.tail == 1)

// assert(t2.)
