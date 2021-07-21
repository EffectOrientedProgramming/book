import scala.math.Pi

def HelloAtomicScala3 =
  if (true)
    print("a")
  else
    print("b")

  val foo: String = "adsf"

  def doStuff: Unit =
    println("A".toString())
    println("adsf" + "bla")

    List(1)
      .map(i =>
        i.toString()
        "adsf" + "bla"
      )

  println("hello atomic scala")
  println("another one!")
  val output = """
    adsf asfd as d
   """

/*
    blah
 */

object Extensions:

  trait PrettyString[A]:
    extension (a: A) def prettyString(): String

  given PrettyString[String] with

    extension (a: String)

      def prettyString(): String =
        "\"" + a + "\""

  given PrettyString[Int] with

    extension (a: Int)

      def prettyString(): String =
        a.toString.prettyString()

  given [A: PrettyString]
      : PrettyString[List[A]] with

    extension (a: List[A])

      def prettyString(): String =
        a.map(_.prettyString()).mkString(",")

class VisibilityClass(name: String)

@main def testExtends() =
  val instance = VisibilityClass("bill")

  val s = "giraffe@56123".split("@") match {
    case a: Array[String | Null] => a.apply(1)
    case _: Null                 => "asdf"
  }
  println(s)

  NoNoNull.definitelyNotNull().toUpperCase()
  //NoNoNull.maybeNotNull().toUpperCase() // does not work because maybeNotNull is String | Null
  NoNoNull.maybeNotNull().nn.toUpperCase()

  val maybeS: String | Null = "asdf"

  // Does not work
  // val o: Option[String] = Option(maybeS)

  // Extension method on objects
  extension (o: Option.type)
    def fromNullable[T](
        input: T | Null
    ): Option[T] =
      if (input != null) Some(input) else None

  /*
      input match
      case null => None
        //       case s: T => Some(s)    This does not work with an explicit T type
      case s: T => Some(s)
   */

  println(
    "Nifty!: " + Option
      .fromNullable(maybeS)
      .map(_.toUpperCase)
  )

  val o: Option[String] = maybeS match
    case s: String => Some(s)
    case null      => None

  println(o)

  "giraffe@56123".dropWhile(_ != '@')
  val intRange: Range.Exclusive = Range(0, 5)
  println(intRange)
  println(Seq(0, 5))

  import scala.math.{Pi => SuperPi}
  import Extensions.given
  println(1.prettyString())
  println("asdf".prettyString())
  println(List("foo", "bar").prettyString())
  println(
    List(List("foo", "bar"), List("asdf"))
      .prettyString()
  )
