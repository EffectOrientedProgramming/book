# Contract-Based Composability

Good contracts make good composability.

contracts are what makes composability work at scale
our effects put in place contracts on how things can compose
exceptions do not put in place a contract

maybe something about how exceptions do not convey a contract in either direction. Anything can be wrapped with a try.  Things that produce exceptions don't need to be wrapped with trys.

possible example of Scope for Environment contracts

possible contract on provide for things not needed

```scala
ZIO.succeed("asdf").someOrFail("error")
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
ZIO.succeed(maybeThing()).someOrFail("error")
// res1: ZIO[Any, String, Unit] = OnSuccess(
//   trace = "repl.MdocSession.MdocApp.res1(07_Composability.md:20)",
//   first = Sync(
//     trace = "repl.MdocSession.MdocApp.res1(07_Composability.md:20)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$14938/0x0000000803da8040@5a252ee0
//   ),
//   successK = zio.ZIO$$Lambda$17780/0x0000000804485840@78bb0984
// )
```


```scala
ZIO
  .succeed(println("Always gonna work"))
  .retryN(100)
// error:
// This error handling operation assumes your effect can fail. However, your effect has Nothing for the error type, which means it cannot fail, so there is no need to handle the failure. To find out which method you can use instead of this operation, please see the reference chart at: https://zio.dev/can_fail.
// I found:
// 
//     CanFail.canFail[E](/* missing */summon[util.NotGiven[E =:= Nothing]])
// 
// But no implicit values were found that match type util.NotGiven[E =:= Nothing].
//     summaryForZ("stock market").run
//
```

```scala
ZIO
  .attempt(println("This might work"))
  .retryN(100)
// res3: ZIO[Any, Throwable, Unit] = OnSuccess(
//   trace = "repl.MdocSession.MdocApp.res3(07_Composability.md:35)",
//   first = Sync(
//     trace = "repl.MdocSession.MdocApp.res3(07_Composability.md:35)",
//     eval = zio.ZIOCompanionVersionSpecific$$Lambda$14938/0x0000000803da8040@11ee4a4a
//   ),
//   successK = zio.ZIO$$$Lambda$14940/0x0000000803dae840@4b02e6ef
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
// TODO Consider deleting .as
//   The problem is we can't return literals in zio-direct.
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
```scala
import scala.concurrent.Future
```

There is a function that returns a Future:



```scala
getHeadLine(): Future[String]
```

```scala
val getHeadlineZ =
  ZIO.from:
    getHeadLine()
```

```scala
runDemo:
  defer:
    getHeadlineZ.run
  .catchAll:
    case _: Throwable =>
      ZIO.debug:
        "Could not fetch the latest headline"
// Battery Breakthrough
```


```scala
findTopicOfInterest("content"): Option[String]
```

```scala
case class NoInterestingTopicsFound()
def topicOfInterestZ(headline: String) =
  ZIO
    .from:
      findTopicOfInterest:
        headline
    .orElseFail:
      NoInterestingTopicsFound()
```

```scala
runDemo:
  defer:
    val headline = getHeadlineZ.run
    topicOfInterestZ(headline).run
  .catchAll:
    case _: Throwable =>
      ZIO.debug:
        "Could not fetch the latest headline"
    case NoInterestingTopicsFound() =>
      ZIO.debug:
        s"No Interesting topic found in the headline"
// No Interesting topic found in the headline
// ()
```


```scala
closeableFile: AutoCloseable
```

```scala
val closeableFileZ =
  ZIO
    .fromAutoCloseable:
      ZIO.succeed:
        closeableFile

```

```scala
runDemo:
  defer:
    closeableFileZ.run
// Closing file now!
// repl.MdocSession$MdocApp$$anon$13@72afc3d2
```

```scala
closeableFile.existsInFile("something"): Boolean
```

```scala
runDemo:
  defer:
    val headline = getHeadlineZ.run
    val topicOfInterest = topicOfInterestZ(headline).run
    val file = closeableFileZ.run
    file.existsInFile(topicOfInterest)
  .catchAll:
    case _: Throwable =>
      ZIO.debug:
        "Could not fetch the latest headline"
    case NoInterestingTopicsFound() =>
      ZIO.debug:
        s"No Interesting topic found in the headline"
// No Interesting topic found in the headline
// ()
```

```scala
case class NoRecordsAvailable(topic: String)
```


```scala
summaryFor("stock market"): Either[NoRecordsAvailable, String]
```

```scala
def summaryForZ(topic: String) =
  ZIO
    .from:
      summaryFor:
        topic
```

```scala
runDemo:
  defer:
    summaryForZ("stock market").run
  .catchAll:
    case NoRecordsAvailable(topic) =>
        ZIO.debug:
          s"No records available for ${topic}"
// detailed history
```

```scala

closeableFile.write("asdf"): Try[Unit]
```


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

## Repeats

Repeating is a form of composability, because you are composing a program with itself


### Injecting Behavior before/after/around




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

import scala.Option
import scala.util.Try

trait NewsService:
  def getHeadline(): Future[String]

case class NoInterestingTopicsFound()
trait ContentAnalyzer:
  def findTopicOfInterest(
      content: String
  ): Option[String]

case class NoRecordsAvailable(topic: String)
trait HistoricalRecord:

  def summaryFor(
      topic: String
  ): Either[NoRecordsAvailable, String]

trait CloseableFile extends AutoCloseable:
  def existsInFile(searchTerm: String): Boolean

  def write(entry: String): Try[Unit]

case class Scenario(
    newsService: NewsService,
    contentAnalyzer: ContentAnalyzer,
    historicalRecord: HistoricalRecord,
    closeableFile: CloseableFile
):
  val headLineZ =
    ZIO.from:
      newsService.getHeadline()

  def topicOfInterestZ(headline: String) =
    ZIO
      .from:
        contentAnalyzer.findTopicOfInterest:
          headline
      .orElseFail:
        NoInterestingTopicsFound()

  val closeableFileZ =
    ZIO.fromAutoCloseable:
      ZIO.succeed:
        closeableFile

  def summaryForZ(topic: String) =
    ZIO.from:
      historicalRecord.summaryFor:
        topic

  def writeToSummaryFileZ(
      summaryFile: CloseableFile,
      content: String
  ) =
    ZIO.from:
      summaryFile.write:
        content

  val logic =
    defer:
      val headline: String = headLineZ.run

      val topic: String =
        topicOfInterestZ(headline).run

      val summaryFile: CloseableFile =
        closeableFileZ.run

      val topicIsFresh: Boolean =
        summaryFile.existsInFile:
          topic

      if (topicIsFresh)
        val newInfo = summaryForZ(topic).run

        writeToSummaryFileZ(summaryFile, newInfo)
          .run

      ZIO
        .debug:
          "topicIsFresh: " + topicIsFresh
        .run

      // todo: some error handling to show that
      // the errors weren't lost along the way
    .catchAll:
      case _: Throwable =>
        ZIO.debug(
          "News Service could not fetch the latest headline"
        )
      case NoRecordsAvailable(topic) =>
        ZIO.debug(
          s"Could not generate a summary for $topic"
        )
      case NoInterestingTopicsFound() =>
        ZIO.debug(
          s"No Interesting topic found in the headline"
        )
end Scenario

object AllTheThings extends ZIOAppDefault:
  type Nail = ZIO.type
  /* If ZIO is your hammer, it's not that you
   * _see_ everything as nails.
   * You can actually _convert_ everything into
   * nails. */

  /* Is Either different enough to demo here?
   * It basically splits the difference between
   * Option/Try I think if we show both of them,
   * we can skip Either. */

  override def run =
    defer:
      val scenario = ZIO.service[Scenario].run
      scenario.logic
    .provide(
      ZLayer.fromFunction(Scenario.apply),
      ZLayer
        .succeed(Implementations.newsService),
      ZLayer.succeed(
        Implementations.contentAnalyzer
      ),
      ZLayer.succeed(
        Implementations.historicalRecord
      ),
      ZLayer
        .succeed(Implementations.closeableFile)
    )
end AllTheThings

```


### experiments/src/main/scala/composability/Invisible.scala
```scala
package composability

import scala.concurrent.Future
import scala.util.Try

object Implementations:
  val newsService =
    new NewsService:
      def getHeadline(): Future[String] =
        Future.successful:
          "The stock market is crashing!"

  val contentAnalyzer =
    new ContentAnalyzer:
      override def findTopicOfInterest(
          content: String
      ): Option[String] =
        Option.when(
          content.contains("stock market")
        ):
          "stock market"

  val historicalRecord =
    new HistoricalRecord:
      override def summaryFor(
          topic: String
      ): Either[NoRecordsAvailable, String] =
        topic match
          case "stock market" =>
            Right("detailed history")
          case "TODO_OTHER_MORE_OBSCURE_TOPIC" =>
            Left(NoRecordsAvailable("blah blah"))
  val closeableFile =
    new CloseableFile:
      override def close =
        println("Closing file now!")

      override def existsInFile(
          searchTerm: String
      ): Boolean = searchTerm == "stock market"

      override def write(
          entry: String
      ): Try[Unit] =
        println("Writing to file: " + entry)
        if (entry == "stock market")
          Try(
            throw new Exception(
              "Stock market already exists!"
            )
          )
        else
          Try(())
end Implementations

```

