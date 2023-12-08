# Composability

An essential part of creating programs is the ability to combine small pieces into larger pieces.  
Different languages / paradigms provide different ways to accomplish these combinations.  
Objects can be combined by creating objects that contain other objects.  
Functions can be combined by creating new functions that call other functions.  
These are types of "composition" but these traditional approaches do not address all of the aspects of a program.

For example, functions that use resources which need to be opened and closed, do not compose.




ZIOs compose including errors, async, blocking, resource managed, cancellation, eitherness, environmental requirements.

## Composability Explanation

1. But Functions & Specialized Data Types Don't Compose for Effects
  1. Composability
    1. Limitations of Functions & SDTs
    1. Some intro to Universal Effect Data Types ie ZIO
    1. The ways in which ZIOs compose (contrasted to limitations)
    1. Note: Merge chapters: composability, Unit, The_ZIO_Type
    1. Note: Avoid explicit anonymous sum & product types at this point

## Alternatives and their downsides

Other framings/techniques and their pros/cons:

### Plain functions that throw Exceptions

- We can't union these error possibilities and track them in the type system
- Cannot attach behavior to deferred functions



### Plain functions that block

- We can't indicate if they block or not
- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments

### Functions that return Either/Option/Try/etc

- We can manage the errors in the type system, but we can't interrupt the code that is producing these values
- All of these types must be manually transformed into the other types
- Execution is not deferred

### Functions that return a Future

- Can be interrupted example1[^^future_interrupted_1] two[^^future_interrupted_2]
- [Cleanup is not guaranteed](./15_Concurrency_Interruption.md##Future-Cancellation)
- Manual management of cancellation
- Start executing immediately
- Must all fail with Exception
- 
### Implicits
  - Are not automatically managed by the compiler, you must explicitly add each one to your parent function
  - Resolving the origin of a provided implicit can be challenging

### Try-with-resources
  - These are statically scoped
  - Unclear who is responsible for acquisition & cleanup



Each of these approaches gives you benefits, but you can't assemble them all together.
Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, and not easily changed.

The number of combinations is something like:
  PairsIn(numberOfConcepts)

## Universal Composability with ZIO

ZIOs compose including errors, async, blocking, resource managed, cancellation, eitherness, environmental requirements.

The types expand through generic parameters. ie composing a ZIO with an error of `String` with a ZIO with an error of `Int` results in a ZIO with an error of `String | Int`.

With functions there is one way to compose.
`f(g(h))` will sequentially apply the functions from the inside out.  
Another term for this form of composition is called `andThen` in Scala.

With ZIO you can use `zio-direct` to compose ZIOs sequentially with:

```scala
runDemo:
  defer:
    val topStory =
      findTopNewsStory
      .run
    textAlert:
      topStory
    .run
// Texting story: Battery Breakthrough
// ()
```

There are many other ways you can compose ZIOs.
The methods for composability depend on the desired behavior.
For example, to compose a ZIO that can produce an error with a ZIO that logs the error and then produces a default value, you can use the `catchAll` like:

```scala
def logAndProvideDefault(e: Throwable) =
  Console
    .printLine:
      e.getMessage
    .as:
      "default value"

runDemo:
  ZIO
    .attempt:
      ???
    .catchAll:
      logAndProvideDefault
// an implementation is missing
// default value
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/05_Composability.md)


## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/composability/AllTheThings.scala
```scala
package composability

import zio.*
import zio.direct.*

import scala.Option
import scala.util.{Success, Try}

// todo: turn into a relatable scenario
// todo: consider a multi-step build like in Superpowers
object AllTheThings extends ZIOAppDefault:

  def asyncThing(i: Int) = ZIO.sleep(i.seconds)

  def optionThing(o: Option[Int]) =
    ZIO.fromOption(o)

  def errorThing[A](t: Try[A]) = ZIO.fromTry(t)

  val resourcefulThing
      : ZIO[Scope, Nothing, String] =
    val open =
      defer:
        Console.printLine("open").orDie.run
        "asdf"

    val close = Console.printLine("close").orDie

    ZIO.acquireRelease(open)(_ => close)

  override def run =
    defer:
      // todo: useful order, maybe async first or
      // near first?
      // maybe something parallel in here too?
      // maybe add Future or make asyncThing a
      // Future
      val s: String = resourcefulThing.run
      val t: Try[String] =
        Success(
          s
        ) // todo: some failable function
      val w: String = ZIO.fromTry(t).run
      val o: Option[Int] =
        Option.unless(w.isEmpty)(
          w.length
        ) // todo: some optional function
      val i: Int = ZIO.fromOption(o).debug.run
      asyncThing(i).run
      // todo: some error handling to show that
      // the errors weren't lost along the way
    .catchAll:
      case t: Throwable =>
        ???
      case _: Any =>
        ???
end AllTheThings

```

