## interpreter-level1_nochaining

 

### experiments/src/main/scala/interpreter/level1_nochaining/1_singleOperation.scala
```scala
package interpreter.level1_nochaining

/* Programs with no chained operations.
 * The interpreter only handles known types. */

trait Operation

case class Print(s: String) extends Operation
case class Random(f: Int => Unit)
    extends Operation
object NoOp extends Operation

def interpret(operation: Operation): Unit =
  operation match
    case p: Print =>
      println(p.s)
    case r: Random =>
      r.f(scala.util.Random.nextInt())

@main
def m1 =
  val p1 = Print("hello")
  val r1 = Random(println)

  interpret(p1)
  interpret(r1)

```


### experiments/src/main/scala/interpreter/level1_nochaining/2_InterpretSequence.scala
```scala
package interpreter.level1_nochaining

def interpretSequence(
    prints: Seq[Operation]
): Unit =
  prints match
    case Nil =>
      ()
    case head :: tail =>
      interpret(head)
      interpretSequence(tail)

@main
def demoSequence =
  val program =
    Seq(
      Print("asdf"),
      Print("hello"),
      Random(println)
    )

  interpretSequence(program)

```


