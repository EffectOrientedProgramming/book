# Composability

Good contracts make good composability.

contracts are what makes composability work at scale
our effects put in place contracts on how things can compose
exceptions do not put in place a contract

possible example of Scope for Environment contracts

possible contract on provide for things not needed


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

## Alternatives and their downsides

Other framings/techniques and their pros/cons:

### Plain functions that return Unit

`Unit` can be viewed as the bare minimum of effect tracking.

Consider a function

```scala mdoc
def saveInformation(info: String): Unit =
  ???
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

{{ todo: example on error channel expansion }}

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

```scala mdoc:runzio
def run =
  defer:
    val topStory =
      findTopNewsStory.run
    textAlert:
      topStory
    .run
```

There are many other ways you can compose ZIOs.
The methods for composability depend on the desired behavior.
For example, to compose a ZIO that can produce an error with a ZIO that logs the error and then produces a default value, you can use the `catchAll` like:


```scala mdoc:runzio
// TODO Consider deleting .as
//   The problem is we can't return literals in zio-direct.
def logAndProvideDefault(e: Throwable) =
  Console
    .printLine:
      e.getMessage
    .as:
      "default value"

def run =
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
var headLineAvailable =
  true
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

```scala mdoc:compile-only
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

```scala mdoc:runzio
def run =
  getHeadlineZ
```
Now let's confirm the behavior when the headline is not available.

```scala mdoc:runzio
// This controls some invisible machinery
headLineAvailable =
  false

def run =
  getHeadlineZ
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

```scala mdoc:runzio
// This controls some invisible machinery
headLineAvailable =
  true

def run =
  topicOfInterestZ:
    "stock market crash!"
```

```scala mdoc:runzio
def run =
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
    override def close =
      println("Closing file!")

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
        contents =
          entry :: contents
        Try(entry)
      }
```

We have an existing function that produces an `AutoCloseable`.

```scala mdoc:compile-only
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

```scala mdoc:runzio
def run =
  closeableFileZ
```

Since that is not terribly useful, let's start calling some methods on our managed file.


```scala mdoc:compile-only
closeableFile().contains("something"): Boolean
```

```scala mdoc:runzio
def run =
  defer:
    val file =
      closeableFileZ.run
    file.contains:
      "topicOfInterest"
```

```scala mdoc:compile-only
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

```scala mdoc:runzio
def run =
  defer:
    val file =
      closeableFileZ.run
    writeToFileZ(file, "New data on topic").run
```


### Either Interop

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

```scala mdoc:compile-only
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

```scala mdoc:runzio
def run =
  summaryForZ:
    "stock market"
```

```scala mdoc:runzio
def run =
  summaryForZ:
    "obscureTopic"
```

Now that we have all of these well-defined effects, we can wield them in any combination and sequence we desire.

```scala mdoc:silent
val researchWorkflow =
  defer:
    val headline: String =
      getHeadlineZ.run

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


```scala mdoc:runzio
def run =
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


## Repeats

Repeating is a form of composability, because you are composing a program with itself


### Injecting Behavior before/after/around


