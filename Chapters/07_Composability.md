# Composability

Good contracts make good composability.

contracts are what makes composability work at scale
our effects put in place contracts on how things can compose
exceptions do not put in place a contract

maybe something about how exceptions do not convey a contract in either direction. 
Anything can be wrapped with a try.  
Things that produce exceptions don't need to be wrapped with trys.

possible example of Scope for Environment contracts

possible contract on provide for things not needed



```scala mdoc:fail
ZIO
  .succeed(println("Always gonna work"))
  .retryN(100)
```

```scala mdoc
ZIO
  .attempt(println("This might work"))
  .retryN(100)
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

```scala mdoc
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

### Implicits
  - Are not automatically managed by the compiler, you must explicitly add each one to your parent function
  - Resolving the origin of a provided implicit can be challenging

### Try-with-resources / AutoCloseable


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
```scala mdoc:invisible
val findTopNewsStory =
  ZIO.succeed:
    "Battery Breakthrough"

def textAlert(message: String) =
  Console.printLine:
    s"Texting story: $message"
```

```scala mdoc
runDemo:
  defer:
    val topStory = findTopNewsStory.run
    textAlert:
      topStory
    .run
```

There are many other ways you can compose ZIOs.
The methods for composability depend on the desired behavior.
For example, to compose a ZIO that can produce an error with a ZIO that logs the error and then produces a default value, you can use the `catchAll` like:


```scala mdoc
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
```

## All The Thing Example
When writing substantial, complex applications, you will quickly encounter APIs that that return data types with less power than ZIO.
Thankfully, ZIO provides numerous conversion methods that simplify these interactions.
By utilizing some clever type-level, compile-time techniques
  (which we will not cover here),
  ZIO is able to use a single interface - `ZIO.from` to handle many of these cases.

## Future interop

###
```scala mdoc
import scala.concurrent.Future
```

The original asynchronous datatype in Scala has several undesirable characteristics:

- Cleanup is not guaranteed
- Start executing immediately
- Must all fail with Exception
- Needs `ExecutionContext`s passed everywhere


There is a function that returns a Future:

```scala mdoc:invisible
var headLineAvailable = true
// TODO If we make this function accept the "mock" result and return that, then
//  we can leverage that to hit all of the possible paths in AllTheThings.
def getHeadLine(): Future[String] =
  if (headLineAvailable)
    Future.successful("stock market crash!")
  else
    Future.failed(
      new Exception("Headline not available")
    )
```

```scala mdoc:silent
getHeadLine(): Future[String]
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala mdoc:silent
case class HeadlineNotAvailable()
val getHeadlineZ =
  ZIO
    .from:
      getHeadLine()
    .mapError:
      case _: Throwable =>
        HeadlineNotAvailable()
```

```scala mdoc
runDemo:
  getHeadlineZ
```
Now let's confirm the behavior when the headline is not available.

```scala mdoc
// This controls some invisible machinery
headLineAvailable = false

runDemo:
  getHeadlineZ
```

```scala mdoc:invisible
// TODO We showed this being toggled off - would it be appropriate to show it turning back on?
headLineAvailable = true
```

### Option Interop
`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, error types, or anything else.
It merely indicates if you have a value.

```scala mdoc:invisible
def findTopicOfInterest(
    content: String
): Option[String] =
  Option.when(content.contains("stock market")):
    "stock market"
```

```scala mdoc:silent
// TODO Discuss colon clashing in this example
val _: Option[String] =
  findTopicOfInterest:
    "content"
```

If you want to treat the case of a missing value as an error, you can again use `ZIO.from`:
ZIO will convert `None` into a generic error type, giving you the opportunity to define a more specific error type.

```scala mdoc
case class NoInterestingTopic()
def topicOfInterestZ(headline: String) =
  ZIO
    .from:
      findTopicOfInterest:
        headline
    .orElseFail:
      NoInterestingTopic()
```

```scala mdoc
runDemo:
  topicOfInterestZ:
    "stock market crash!"
```

```scala mdoc
runDemo:
  topicOfInterestZ:
    "boring and inane content"
```

### AutoCloseable Interop
Java/Scala provide the `AutoCloseable` interface for defining finalizer behavior on objects.
While this is a big improvement over manually managing this in ad-hoc ways, the static scoping of this mechanism makes it clunky to use.
TODO Decide whether to show nested files example to highlight this weakness

```scala mdoc:invisible
import scala.util.Try

trait CloseableFile extends AutoCloseable:
  // TODO Return existing entry, rather than a
  // raw Boolean?
  def contains(searchTerm: String): Boolean
  def write(entry: String): Try[String]

def closeableFile() =
  new CloseableFile:
    var contents: List[String] =
      List("Medical Breakthrough!")
    println("Opening file!")
    override def close = println("Closing file!")

    override def contains(
        searchTerm: String
    ): Boolean =
      println:
        "Searching file for: " + searchTerm
      searchTerm == "stock market"

    override def write(
        entry: String
    ): Try[String] =
      if (entry == "stock market")
        Try(
          throw new Exception(
            "Stock market already exists!"
          )
        )
      else {
        println("Writing to file: " + entry)
        contents = entry :: contents
        Try(entry)
      }
```

We have an existing function that produces an `AutoCloseable`.

```scala mdoc:silent
closeableFile(): AutoCloseable
```

Since `AutoCloseable` is a trait that can be implemented by arbitrary classes, we can't rely on `ZIO.from` to automatically manage this conversion for us.
In this situation, we should use the explicit `ZIO.fromAutoCloseable` function.

```scala mdoc:silent
val closeableFileZ =
  ZIO.fromAutoCloseable:
    ZIO.succeed:
      closeableFile()
```

Once we do this, the `ZIO` runtime will manage the lifecycle of this object via the `Scope` mechanism.
TODO Link to docs for this?
In the simplest case, we open and close the file, with no logic while it is iopen.

```scala mdoc
runDemo:
  closeableFileZ
```

Since that is not terribly useful, let's start calling some methods on our managed file.


```scala mdoc:silent
closeableFile().contains("something"): Boolean
```

```scala mdoc
runDemo:
  defer:
    val file = closeableFileZ.run
    file.contains:
      "topicOfInterest"
```

```scala mdoc:silent
closeableFile().write("asdf"): Try[String]
```

```scala mdoc
def writeToFileZ(
    file: CloseableFile,
    content: String
) =
  ZIO
    .from:
      file.write:
        content
    .orDie
```

```scala mdoc
runDemo:
  defer:
    val file = closeableFileZ.run
    writeToFileZ(file, "New data on topic").run
```

```scala mdoc
case class NoRecordsAvailable(topic: String)
```

```scala mdoc:invisible
import scala.util.Either
def summaryFor(
    topic: String
): Either[NoRecordsAvailable, String] =
  topic match
    case "stock market" =>
      Right:
        s"detailed history of $topic"
    case "obscureTopic" =>
      Left:
        NoRecordsAvailable:
          "obscureTopic"
```

```scala mdoc:silent
summaryFor("stock market"): Either[
  NoRecordsAvailable,
  String
]
```

```scala mdoc
def summaryForZ(topic: String) =
  ZIO.from:
    summaryFor:
      topic
```

```scala mdoc
runDemo:
  summaryForZ:
    "stock market"
```

```scala mdoc
runDemo:
  summaryForZ:
    "obscureTopic"
```

Now that we have all of these well-defined effects, we can wield them in any combination and sequence we desire.

```scala mdoc:silent
val researchWorkflow =
  defer:
    val headline: String = getHeadlineZ.run

    val topic: String =
      topicOfInterestZ:
        headline
      .run

    val summaryFile: CloseableFile =
      closeableFileZ.run

    val topicIsFresh: Boolean =
      summaryFile.contains:
        topic

    if (topicIsFresh)
      val newInfo =
        summaryForZ:
          topic
        .run

      writeToFileZ(summaryFile, newInfo).run
      newInfo
    else
      "Topic was already covered"
```


```scala mdoc
runDemo:
  researchWorkflow
    // todo: some error handling to show that
    // the errors weren't lost along the way
    .mapError:
      case HeadlineNotAvailable() =>
        "Could not fetch headline"
      case NoRecordsAvailable(topic) =>
        s"No records for $topic"
      case NoInterestingTopic() =>
        "No Interesting topic found"
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


