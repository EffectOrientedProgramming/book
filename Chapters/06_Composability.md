# Composability

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

```scala mdoc:invisible
enum Scenario:
  case StockMarketHeadline
  case HeadlineUnavailable
```

ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing substantial, complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

### Future interop

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
// TODO If we make this function accept the "mock" result and return that, then
//  we can leverage that to hit all of the possible paths in AllTheThings.
def getHeadLine(scenario: Scenario): Future[String] =
  if (scenario == Scenario.StockMarketHeadline)
    Future.successful("stock market crash!")
  else
    Future.failed(
      new Exception("Headline not available")
    )
```

```scala mdoc:compile-only
getHeadLine(???): Future[String]
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala mdoc:silent
case class HeadlineNotAvailable()
def getHeadlineZ(scenario: Scenario) =
  ZIO
    .from:
      getHeadLine(scenario)
    .mapError:
      case _: Throwable =>
        HeadlineNotAvailable()
```

```scala mdoc:runzio
def run =
  getHeadlineZ(Scenario.StockMarketHeadline)
```
Now let's confirm the behavior when the headline is not available.

```scala mdoc:runzio
// This controls some invisible machinery
headLineAvailable =
  false

def run =
  getHeadlineZ(Scenario.HeadlineUnavailable)
```

### Option Interop
`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, error types, or anything else.
It merely indicates that a value might not be available.

- Execution is not deferred
- Cannot interrupt the code that is producing these values

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

### Either Interop

- Execution is not deferred
- Cannot interrupt the code that is producing these values

```scala mdoc
case class NoRecordsAvailable(topic: String)
```

```scala mdoc:invisible
import scala.util.Either
def wikiArticle(
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
wikiArticle("stock market"): Either[
  NoRecordsAvailable,
  String
]
```

```scala mdoc
def wikiArticleZ(topic: String) =
  ZIO.from:
    wikiArticle:
      topic
```

```scala mdoc:runzio
def run =
  wikiArticleZ:
    "stock market"
```

```scala mdoc:runzio
def run =
  wikiArticleZ:
    "obscureTopic"
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
  def summaryFor(searchTerm: String): String

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
      
    override def summaryFor(searchTerm: String): String =
      if (searchTerm == "stock market") 
        "stock markets are neat"
      else
        throw Exception(s"No summary available for $searchTerm")
      
    

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
In the simplest case, we open and close the file, with no logic while it is open.

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

### Plain functions that throw Exceptions

```scala mdoc:compile-only
closeableFile().summaryFor("asdf"): String
```

```scala mdoc
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
```scala mdoc:invisible
def summarize(article: String): String =
  println("AI summarizing: start")
  // Represents the AI taking a long time to summarize the content
  if (!article.contains("stock market")) 
    Thread.sleep(1000)
  
  println("AI summarizing: complete")
  s"TODO Summarized content"
```


```scala mdoc:compile-only
// TODO Can we use silent instead of compile-only above?
summarize("some topic"): String
```

This gets interrupted, although it takes a big performance hit
```scala mdoc
case class AIFailure()

def summarizeZ(article: String) =
  ZIO
    .attemptBlockingInterrupt:
      summarize(article)
    .onInterrupt(ZIO.debug("Interrupted summarize"))
    .mapError(_ => AIFailure())
```


- We can't indicate if they block or not
- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments

### Sequencing 
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
    textAlert(topStory).run
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

```scala mdoc:silent
def researchHeadline(scenario: Scenario) =
  defer:
    val headline: String =
      getHeadlineZ(scenario).run

    val topic: String =
      topicOfInterestZ(headline).run

    val summaryFile: CloseableFile =
      closeableFileZ.run

    val topicIsFresh: Boolean =
      summaryFile.contains:
        topic

    if (topicIsFresh)
      val wikiArticle =
        wikiArticleZ(topic).run

      val summary = summarizeZ(wikiArticle).run
      writeToFileZ(summaryFile, summary).run
      summary
    else
      summaryForZ(summaryFile, topic).run
```


```scala mdoc:runzio
// todo intermediate value with error handling before we start demo'ing all paths
def run =
  researchHeadline(Scenario.StockMarketHeadline)
    // todo: some error handling to show that
    // the errors weren't lost along the way
    .mapError:
      case HeadlineNotAvailable() =>
        "Could not fetch headline"
      case NoRecordsAvailable(topic) =>
        s"No records for $topic"
      case NoInterestingTopic() =>
        "No Interesting topic found"
      case AIFailure() =>
        "Error during AI summary"
      case NoSummaryAvailable(topic) =>
        s"No summary available for $topic"
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

```scala mdoc
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
