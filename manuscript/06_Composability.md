# Composability


[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/06_Composability.md)


An essential part of creating programs is the ability to combine small pieces into larger pieces.  
We call this *composability*.
This might seem so simple that it must be a solved problem.

Languages and libraries provide different ways to enable composability.

- Objects can be composed by putting objects inside other objects.
- Functions can be composed by calling functions within other functions.

These approaches do not address all aspects of composition.
For example, you cannot compose functions using resources that need to be opened and closed.
Issues that complicate composition include:

- errors
- async
- blocking
- managed resource
- cancellation
- either-ness
- environmental requirements

These concepts and their competing solutions will be expanded on and contrasted with ZIO throughout this chapter.


## Universal Composability with ZIO (All The Thing Example)


ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing substantial, complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

### Future interop

```scala
import scala.concurrent.Future
```

The original asynchronous datatype in Scala has several undesirable characteristics:

- Cleanup is not guaranteed
- Start executing immediately
- Must all fail with Exception
- Needs `ExecutionContext`s passed everywhere


There is a function that returns a Future:


```scala
getHeadLine(???): Future[String]
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala
case class HeadlineNotAvailable()
def getHeadlineZ(scenario: Scenario) =
  ZIO
    .from:
      getHeadLine(scenario)
    .mapError:
      case _: Throwable =>
        HeadlineNotAvailable()
```

```scala
def run =
  getHeadlineZ(Scenario.StockMarketHeadline)
// Result: stock market crash!
```
Now let's confirm the behavior when the headline is not available.

```scala
def run =
  getHeadlineZ(Scenario.HeadlineUnavailable)
// Result: HeadlineNotAvailable()
```

### Option Interop
`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, error types, or anything else.
It merely indicates that a value might not be available.

- Execution is not deferred
- Cannot interrupt the code that is producing these values


```scala
// TODO Discuss colon clashing in this example
val _: Option[String] =
  findTopicOfInterest:
    "content"
```

If you want to treat the case of a missing value as an error, you can again use `ZIO.from`:
ZIO will convert `None` into a generic error type, giving you the opportunity to define a more specific error type.

```scala
case class NoInterestingTopic()
def topicOfInterestZ(headline: String) =
  ZIO
    .from:
      findTopicOfInterest:
        headline
    .orElseFail:
      NoInterestingTopic()
```

```scala
def run =
  topicOfInterestZ:
    "stock market crash!"
// Result: stock market
```

```scala
def run =
  topicOfInterestZ:
    "boring and inane content"
// Result: NoInterestingTopic()
```

### Either Interop

- Execution is not deferred
- Cannot interrupt the code that is producing these values

```scala
wikiArticle(???): Either[
  Scenario.NoWikiArticleAvailable,
  String
]
```

```scala
def wikiArticleZ(topic: String) =
  ZIO.from:
    wikiArticle:
      topic
```

```scala
def run =
  wikiArticleZ:
    "stock market"
// Result: detailed history of stock market
```

```scala
def run =
  wikiArticleZ:
    "obscureTopic"
// TODO Handle long line. 
// Truncating for now: 
// Defect: scala.MatchError: obscureTopic (of clas
// Result: Defect: scala.MatchError: obscureTopic (of cla
```

### AutoCloseable Interop
Java/Scala provide the `AutoCloseable` interface for defining finalizer behavior on objects.
While this is a big improvement over manually managing this in ad-hoc ways, the static scoping of this mechanism makes it clunky to use.
TODO Decide whether to show nested files example to highlight this weakness


We have an existing function that produces an `AutoCloseable`.

```scala
closeableFile(): AutoCloseable
```

Since `AutoCloseable` is a trait that can be implemented by arbitrary classes, we can't rely on `ZIO.from` to automatically manage this conversion for us.
In this situation, we should use the explicit `ZIO.fromAutoCloseable` function.

```scala
val closeableFileZ =
  ZIO.fromAutoCloseable:
    ZIO.succeed:
      closeableFile()
```

Once we do this, the `ZIO` runtime will manage the lifecycle of this object via the `Scope` mechanism.
TODO Link to docs for this?
In the simplest case, we open and close the file, with no logic while it is open.

```scala
def run =
  closeableFileZ
// Opening file!
// Closing file!
// Result: repl.MdocSession$MdocApp$$anon$19@74c46129
```

Since that is not terribly useful, let's start calling some methods on our managed file.


```scala
closeableFile().contains("something"): Boolean
```

```scala
def run =
  defer:
    val file =
      closeableFileZ.run
    file.contains:
      "topicOfInterest"
// Opening file!
// Searching file for: topicOfInterest
// Closing file!
// Result: false
```

```scala
closeableFile().write("asdf"): Try[String]
```

```scala
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

```scala
def run =
  defer:
    val file =
      closeableFileZ.run
    writeToFileZ(file, "New data on topic").run
// Opening file!
// Writing to file: New data on topic
// Closing file!
// Result: New data on topic
```

### Plain functions that throw Exceptions

```scala
closeableFile().summaryFor("asdf"): String
```

```scala
case class NoSummaryAvailable(topic: String) 
def summaryForZ(
    file: CloseableFile,
    // TODO Consider making a CloseableFileZ
    topic: String
) =
  ZIO.attempt:
    file.summaryFor(topic)
  .mapError(_ => NoSummaryAvailable(topic))
```

TODO:
 - original function: File.summaryFor
 - wrap with ZIO
 - call zio version in AllTheThings

Downsides:
- We cannot union these error possibilities and track them in the type system
- Cannot attach behavior to deferred functions
- do not put in place a contract

### Plain blocking functions
TODO Decide example functionality
- AI analysis of news content?

TODO Prose about the long-running AI process here


```scala
// TODO Can we use silent instead of compile-only above?
summarize("some topic"): String
```

This gets interrupted, although it takes a big performance hit
```scala
def summarizeZ(article: String) =
  ZIO
    .attemptBlockingInterrupt:
      summarize(article)
    .onInterrupt:
      ZIO.debug("Interrupt AI!")
    .orDie // TODO Confirm we don't care about this case. 
    .timeoutFail(Scenario.AITooSlow())(50.millis)
```


- We can't indicate if they block or not
- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments

### Sequencing 
Another term for this form of composition is called `andThen` in Scala.

With ZIO you can use `zio-direct` to compose ZIOs sequentially with:

```scala
def run =
  defer:
    val topStory =
      findTopNewsStory.run
    textAlert(topStory).run
// Texting story: Battery Breakthrough
// Result: ()
```

### Final Collective Criticism
Each of original approaches gives you benefits, but you can't easily assemble a program that utilizes all of them.
They must be manually transformed into each other .

Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, and not easily changed.

The number of combinations is something like:
  PairsIn(numberOfConcepts)


### Fully Composed

Now that we have all of these well-defined effects, we can wield them in any combination and sequence we desire.

```scala
def researchHeadlineRaw(scenario: Scenario) =
  defer:
    val headline: String = // Was a Future
      getHeadlineZ(scenario).run

    val topic: String = // Was an Option
      topicOfInterestZ(headline).run 

    val summaryFile: CloseableFile = // Was an AutoCloseable
      closeableFileZ.run

    val topicIsFresh: Boolean =
      summaryFile.contains:
        topic

    if (topicIsFresh)
      val wikiArticle = // Was an Either
        wikiArticleZ(topic).run

      val summary =  // Was slow, blocking
        summarizeZ(wikiArticle).run
        
      // Was a Try
      writeToFileZ(summaryFile, summary).run
      summary
    else
      // Was throwing
      summaryForZ(summaryFile, topic).run
```

```scala
def researchHeadline(scenario: Scenario) =
  researchHeadlineRaw(scenario)
    .mapError:
      case HeadlineNotAvailable() =>
        "Could not fetch headline"
      case NoInterestingTopic() =>
        "No Interesting topic found"
      case Scenario.AITooSlow() =>
        "Error during AI summary"
      case NoSummaryAvailable(topic) =>
        s"No summary available for $topic"
      case Scenario.NoWikiArticleAvailable() =>
        "No wiki article available"
```

```scala
def run =
  researchHeadline:
    Scenario.StockMarketHeadline
// Opening file!
// Searching file for: stock market
// AI summarizing: start
// AI summarizing: complete
// Interrupt AI!
// Closing file!
// Result: Error during AI summary
```

```scala
def run =
  researchHeadline:
    Scenario.HeadlineUnavailable
// Result: Could not fetch headline
```

```scala
def run =
  researchHeadline:
    Scenario.NoWikiArticleAvailable()
// Opening file!
// Searching file for: barn
// Closing file!
// Result: No wiki article available
```

```scala
def run =
  researchHeadline:
    Scenario.AITooSlow()
// Opening file!
// Searching file for: space
// AI summarizing: start
// Interrupt AI!
// Closing file!
// Result: Error during AI summary
```


## Repeats

Repeating is a form of composability, because you are composing a program with itself


## Injecting Behavior before/after/around


# Graveyard candidates

## Contract-based prose
Good contracts make good composability.

contracts are what makes composability work at scale
our effects put in place contracts on how things can compose


is this about surfacing the hidden information through a "bookkeeper" that conveys the
constraints to the caller



### Plain functions that return Unit TODO Incorporate to AllTheThings

`Unit` can be viewed as the bare minimum of effect tracking.

Consider a function

```scala
def saveInformation(info: String): Unit =
  ???
```

If we look only at the types, this function is an `String=>Unit`.
`Unit` is the single, blunt tool to indicate effectful functions in plain Scala.
When we see it, we know that *some* type of side-effect is being performed.

When a function returns `Unit`, we know that the only reason we are calling the function is to perform an effect.
Alternatively, if there are no arguments to the function, then the input is `Unit`, indicating that an effect is used to _produce_ the result.

Unfortunately, we can't do things like timeout/race/etc these functions. 
We can either execute them, or not, and that's about it, without resorting to additional tools for manipulating their execution.


### CatchAll log example
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

def run =
  ZIO
    .attempt:
      ???
    .catchAll:
      logAndProvideDefault
// an implementation is missing
// Result: default value
```
