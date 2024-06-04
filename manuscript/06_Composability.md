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

When writing complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

### Existing Code

We will utilize several pre-defined functions to highlight less-complete effect alternatives.


### Future

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
val future: Future[String] = getHeadLine(???)
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala
def getHeadlineZ(scenario: Scenario) =
  ZIO
    .from:
      getHeadLine(scenario)
    .mapError:
      case _: Throwable =>
        Scenario.HeadlineNotAvailable
```

```scala
def run =
  getHeadlineZ(Scenario.StockMarketHeadline)
// Network - Getting headline
// Result: stock market rising!
```

Now let's confirm the behavior when the headline is not available.

```scala
def run =
  getHeadlineZ(Scenario.HeadlineNotAvailable)
// Network - Getting headline
// Result: HeadlineNotAvailable
```

### Option

`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, error types, or anything else.
It merely indicates that a value might not be available.

- Execution is not deferred
- Cannot interrupt the code that is producing these values

```scala
val result: Option[String] =
  findTopicOfInterest:
    "content"
```

If you want to treat the case of a missing value as an error, you can again use `ZIO.from`:
ZIO will convert `None` into a generic error type, giving you the opportunity to define a more specific error type.

```scala
def topicOfInterestZ(headline: String) =
  ZIO
    .from:
      findTopicOfInterest:
        headline
    .orElseFail:
      Scenario.NoInterestingTopic()
```

```scala
def run =
  topicOfInterestZ:
    "stock market rising!"
// Analytics - Scanning
// Result: stock market
```

```scala
def run =
  topicOfInterestZ:
    "boring and inane content"
// Analytics - Scanning
// Result: NoInterestingTopic()
```

### Either

- Execution is not deferred
- Cannot interrupt the code that is producing these values

We have an existing function `wikiArticle` that checks for articles on a topic:

```scala
val wikiResult: Either[
  Scenario.NoWikiArticleAvailable,
  String
] =
  wikiArticle("stock market")
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
// Wiki - articleFor(stock market)
// Result: detailed history of stock market
```

```scala
def run =
  wikiArticleZ:
    "barn"
// Wiki - articleFor(barn)
// Result: NoWikiArticleAvailable()
```

### AutoCloseable

Java/Scala provide the `AutoCloseable` interface for defining finalizer behavior on objects.
While this is a big improvement over manually managing this in ad-hoc ways, the static scoping of this mechanism makes it clunky to use.

TODO Decide whether to show nested files example to highlight this weakness


We have an existing function that produces an `AutoCloseable`.

```scala
val file: AutoCloseable =
  openFile("file1")
```

Since `AutoCloseable` is a trait that can be implemented by arbitrary classes, we can't rely on `ZIO.from` to automatically manage this conversion for us.
In this situation, we should use the explicit `ZIO.fromAutoCloseable` function.

```scala
def openFileZ(path: String) =
  ZIO.fromAutoCloseable:
    ZIO.succeed:
      openFile(path)
```

Once we do this, the `ZIO` runtime will manage the lifecycle of this object via the `Scope` mechanism.
TODO Link to docs for this?

Now we open a `File`, and check if it contains a topic of interest.

```scala
def run =
  defer:
    val file =
      openFileZ("file1.txt").run
    file.contains:
      "topicOfInterest"
// File - OPEN
// File - contains(topicOfInterest)
// File - CLOSE
// Result: false
```

Now we highlight the difference between the static scoping of `Using` or `ZIO.fromAutoCloseable`.

```scala
// This was previously-compile only
// The output is too long to fit on a page, 
// and beyond our ability to control
// without resorting to something like pprint.

import scala.util.Using
import java.io.FileReader

Using(openFile("file1.txt")) {
  file1 =>
    Using(openFile("file2.txt")) {
      file2 =>
        file1.sameContent(file2)
    }
}
```

With each new file we open, we have to nest our code deeper.

```scala
def run =
  defer:
    val file1 =
      openFileZ("file1.txt").run
    val file2 =
      openFileZ("file2.txt").run
    file1.sameContent(file2)
// File - OPEN
// File - OPEN
// side-effect print: comparing content
// File - CLOSE
// File - CLOSE
// Result: true
```

Our code remains flat.

### Try

Next we want to write to our `File`.
The existing API uses a `Try` to indicate success or failure.

```scala
val writeResult: Try[String] =
  openFile("file1").write("asdf")
```

```scala
def writeToFileZ(file: File, content: String) =
  ZIO
    .from:
      file.write:
        content
    .mapError:
      _ => Scenario.DiskFull()
```

```scala
def run =
  defer:
    val file =
      openFileZ("file1").run
    writeToFileZ(file, "New data on topic").run
// File - OPEN
// File - write: New data on topic
// File - CLOSE
// Result: New data on topic
```

### Functions that throw

```scala
val summary: String = 
  openFile("file1").summaryFor("asdf")
```

```scala
case class NoSummaryAvailable(topic: String)
def summaryForZ(
    file: File,
    // TODO Consider making a CloseableFileZ
    topic: String
) =
  ZIO
    .attempt:
      file.summaryFor(topic)
    .mapError:
      _ => NoSummaryAvailable(topic)
```

TODO:

- original function: File.summaryFor
- wrap with ZIO
- call zio version in AllTheThings

Downsides:

- We cannot union these error possibilities and track them in the type system
- Cannot attach behavior to deferred functions
- do not put in place a contract

### Slow, blocking functions

TODO Decide example functionality

- AI analysis of news content?

TODO Prose about the long-running AI process here


```scala
// TODO Can we use silent instead of compile-only above?
val summary: String = summarize("topic")
// AI - summarize - start
// AI - summarize - end
// summary: String = "topic summary"
```

This gets interrupted, although it takes a big performance hit

```scala
def summarizeZ(article: String) =
  ZIO
    .attemptBlockingInterrupt:
      summarize(article)
    .onInterrupt:
      ZIO.debug("AI **INTERRUPTED**")
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
```

#### Short-circuiting

Short-circuiting is an essential part Effect Systems because they enable a linear sequence of expressions which helps make code much easier to understand.
The explicit knowledge of exactly how each Effect can fail is part of definition of the Effect.

In order for Effect Systems to have recovery operations, they must know when failure happens.

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
def researchHeadline(scenario: Scenario) =
  defer:
    val headline: String =
      getHeadlineZ(scenario).run

    val topic: String =
      topicOfInterestZ(headline).run

    val summaryFile: File =
      // TODO Use Scenario to determine file?
      openFileZ("file1.txt").run
      
    // TODO Use 2 files at once, to further highlight the dynamic scoping?
    // Not sure if that is too noisy for this flow
    // Maybe something like a cache check if time has passed?

    val knownTopic: Boolean =
      summaryFile.contains:
        topic

    if (knownTopic)
      summaryForZ(summaryFile, topic).run
    else
      val wikiArticle: String =
        wikiArticleZ(topic).run

      val summary: String =
        summarizeZ(wikiArticle).run

      writeToFileZ(summaryFile, summary).run
      summary
```

```scala
def run =
  researchHeadline:
    Scenario.HeadlineNotAvailable
// Network - Getting headline
// Result: HeadlineNotAvailable
```

```scala
def run =
  researchHeadline:
    Scenario.NoInterestingTopic()
// Network - Getting headline
// Analytics - Scanning
// Result: NoInterestingTopic()
```

```scala
def run =
  researchHeadline:
    Scenario.SummaryReadThrows()
// Network - Getting headline
// Analytics - Scanning
// File - OPEN
// File - contains(unicode)
// File - summaryFor(unicode)
// File - CLOSE
// Result: NoSummaryAvailable(unicode)
```

```scala
def run =
  researchHeadline:
    Scenario.NoWikiArticleAvailable()
// Network - Getting headline
// Analytics - Scanning
// File - OPEN
// File - contains(barn)
// Wiki - articleFor(barn)
// File - CLOSE
// Result: NoWikiArticleAvailable()
```

```scala
def run =
  researchHeadline:
    Scenario.AITooSlow()
// Network - Getting headline
// Analytics - Scanning
// File - OPEN
// File - contains(space)
// Wiki - articleFor(space)
// AI **INTERRUPTED**
// File - CLOSE
// Result: AITooSlow()
```

```scala
def run =
  researchHeadline:
    // TODO Handle inconsistency in this example
    // AI keeps timing out
    Scenario.DiskFull()
// Network - Getting headline
// Analytics - Scanning
// File - OPEN
// File - contains(genome)
// Wiki - articleFor(genome)
// AI **INTERRUPTED**
// File - CLOSE
// Result: AITooSlow()
```

And finally, we see the longest, successful pathway through our application:

```scala
def run =
  researchHeadline:
    Scenario.StockMarketHeadline
// Network - Getting headline
// Analytics - Scanning
// File - OPEN
// File - contains(stock market)
// Wiki - articleFor(stock market)
// AI - summarize - start
// AI - summarize - end
// File - write: market is not rational
// File - CLOSE
// Result: market is not rational
```

## Repeats

Repeating is a form of composability, because you are composing a program with itself

## Graveyard candidates

### Plain functions that return Unit

{{TODO Decide if this section is worth keeping. If so, where?}}

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
