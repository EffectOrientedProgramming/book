package effects

import scala.util.Random

def lucky(
    i: Int
)(using random: Random): Boolean =
  random.nextInt(i) ==
    35 // 35 is the magic number

@main
def amilucky =
  // provide a static seed
  given Random = Random(1)
  println(lucky(50))

  val o =
    for
      o1 <- Option("asdf")
      if o1.startsWith("b")
      o2 <- Option(o1.toUpperCase)
    yield o2

  println(o)
end amilucky
