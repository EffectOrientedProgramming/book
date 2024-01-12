# Contract-Based Composability

Good contracts make good composability.

contracts are what makes composability work at scale
our effects put in place contracts on how things can compose
exceptions do not put in place a contract

maybe something about how exceptions do not convey a contract in either direction. Anything can be wrapped with a try.  Things that produce exceptions don't need to be wrapped with trys.

possible example of Scope for Environment contracts

possible contract on provide for things not needed

```scala
ZIO.succeed("asdf")
  .someOrFail("error")
// error:
// 
// This operator requires that the output type be a subtype of Option[Any]
// But the actual type was String..
// I found:
// 
//     IsSubtypeOfOutput.impl[A, B](/* missing */summon[A <:< B])
// 
// But no implicit values were found that match type A <:< B.
// def logAndProvideDefault(e: Throwable) =
//
```

this works as the contract is that the


```scala
ZIO.succeed(maybeThing())
  .someOrFail("error")
// res1: ZIO[Any, String, Unit] = OnSuccess(
//   trace = "repl.MdocSession.MdocApp.res1(07_Composability.md:20)",
//   first = Sync(
//     trace = "repl.MdocSession.MdocApp.res1(07_Composability.md:19)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$15962/0x00000008040a1440@49e1f2e0
//   ),
//   successK = zio.ZIO$$Lambda$17148/0x00000008042c6840@ed8e39e
// )
```


```scala
ZIO.succeed(println("Always gonna work"))
  .retryN(100)
// error:
// This error handling operation assumes your effect can fail. However, your effect has Nothing for the error type, which means it cannot fail, so there is no need to handle the failure. To find out which method you can use instead of this operation, please see the reference chart at: https://zio.dev/can_fail.
// I found:
// 
//     CanFail.canFail[E](/* missing */summon[util.NotGiven[E =:= Nothing]])
// 
// But no implicit values were found that match type util.NotGiven[E =:= Nothing].
// def logAndProvideDefault(e: Throwable) =
//
```

```scala
ZIO.attempt(println("This might work"))
  .retryN(100)
// res3: ZIO[Any, Throwable, Unit] = OnSuccess(
//   trace = "repl.MdocSession.MdocApp.res3(07_Composability.md:34)",
//   first = Sync(
//     trace = "repl.MdocSession.MdocApp.res3(07_Composability.md:34)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$15962/0x00000008040a1440@b97ca29
//   ),
//   successK = zio.ZIO$$$Lambda$15964/0x00000008040a6840@4de40c80
// )
```

is this about surfacing the hidden information through a "bookkeeper" that conveys the
constraints to the caller



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

### Plain functions that return Unit

`Unit` can be viewed as the bare minimum of effect tracking.

Consider a function

```scala
def saveInformation(info: String): Unit = ???
```

If we look only at the types, this function is an `Any=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we see it, we know that *some* type of side-effect is being performed.

When a function returns `Unit`, we know that the only reason we are calling the function is to perform an effect.
Alternatively, if there are no arguments to the function, then the input is `Unit`, indicating that an effect is used to _produce_ the result.

Unfortunately, we can't do things like timeout/race/etc these functions. 
We can either execute them, or not, and that's about it, without resorting to additional tools for manipulating their execution.

### Plain functions that throw Exceptions

- We cannot union these error possibilities and track them in the type system
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
    val topStory = findTopNewsStory.run
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

## All The Thing Example

....


## Hedging 
TODO Determine final location for Hedging
### Why?
This technique snips off the most extreme end of the latency tail.

Determine the average response time for the top 50% of your requests.
If you make a call that does not get a response within this average delay, make an additional, identical request.
However, you do not give up on the 1st request, since it might respond immediately after sending the 2nd.
Instead, you want to race them and use the first response you get

To be clear - this technique will not reduce the latency of the fastest requests at all.
It only alleviates the pain of the slowest responses.

You have `1/n` chance of getting the worst case response time.
This approach turns that into a `1/n^2` chance.
The cost of this is only ~3% more total requests made. *Citations needed*

Further, if this is not enough to completely eliminate your extreme tail, you can employ the exact same technique once more.
Then, you end up with `1/n^3` chance of getting that worst performance.








## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/07_Composability.md)


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

import scala.concurrent.Future
import zio.direct.*

import java.lang.AutoCloseable
import scala.Option
import scala.util.{Success, Try}

// todo: turn into a relatable scenario
// todo: consider a multi-step build like in Superpowers

object AllTheThings extends ZIOAppDefault:
  type Nail = ZIO.type
  /* If ZIO is your hammer, it's not that you
   * _see_ everything as nails.
   * You can actually _convert_ everything into
   * nails. */

  /*  Possible scenario:
   * Get headline - Future Analyze for
   * topic/persons of interest - Option Check if
   * we have made an entry for them in today's
   * summary file - Resource If not:
   * Dig up supporting information on the topic
   * from a DB - Try Make new entry in today's
   * summary file - Resource
   *
   * Is Either different enough to demo here?
   * It basically splits the difference between
   * Option/Try I think if we show both of them,
   * we can skip Either. */

  def getHeadline(): Future[String] =
    Future.successful(
      "The stock market is crashing!"
    )

  def findTopicOfInterest(
      content: String
  ): Option[String] =
    Option
      .when(content.contains("stock market")):
        "stock market"

  trait CloseableFile extends AutoCloseable:
    def existsInFile(searchTerm: String): Boolean

    def close: Unit
    def write(entry: String): Unit

  val summaryFile: CloseableFile =
    new CloseableFile:
      override def close =
        println("Closing file now!")

      override def existsInFile(
          searchTerm: String
      ): Boolean = searchTerm == "stock market"

      override def write(entry: String) = ???

  def asyncThing(i: Int) = ZIO.sleep(i.seconds)

  val resourcefulThing =
    val open =
      defer:
        Console.printLine("open").orDie.run
        "asdf"

    val close =
      (_: Any) =>
        Console.printLine("close").orDie

    ZIO.acquireRelease(open)(close)

  override def run =
    defer:
      // todo: useful order, maybe async first or
      // near first?
      // maybe something parallel in here too?
      // Convert from AutoCloseable
      // maybe add Future or make asyncThing a
      // Future `
      val headline: String =
        ZIO
          .fromFuture: implicit ec =>
            getHeadline()
          .run

      val topic =
        ZIO
          .fromOption:
            findTopicOfInterest(headline)
          .run

      val summaryFileZ =
        ZIO
          .fromAutoCloseable:
            ZIO.succeed:
              summaryFile
          .run

      val t: Try[String] = Success(headline)
      // todo: some failable function
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

def futureBits =
  ZIO.fromFuture(implicit ec =>
    Future.successful("Success!")
  )
  ZIO.fromFuture(implicit ec =>
    Future.failed(new Exception("Failure :("))
  )

```

