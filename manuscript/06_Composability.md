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

## Universal Composability


ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

## Existing Code

{{ TODO: subhead rename and what else needs to be here? }}

We will utilize several pre-defined functions to highlight less-complete effect alternatives.


## Future

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
val future: Future[String] =
  getHeadLine()
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala
def getHeadlineZ() =
  ZIO
    .from:
      getHeadLine()
    .mapError:
      case _: Throwable =>
        HeadlineNotAvailable
```

```scala
override val bootstrap = stockMarketHeadline

def run =
  getHeadlineZ()
```

Output:

```shell
Network - Getting headline
Result: stock market rising!
```

Now let's confirm the behavior when the headline is not available.

```scala
override val bootstrap = headlineNotAvailable

def run =
  getHeadlineZ()
```

Output:

```shell
Network - Getting headline
Result: HeadlineNotAvailable
```

## Option

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
      NoInterestingTopic()
```

```scala
def run =
  topicOfInterestZ:
    "stock market rising!"
```

Output:

```shell
Analytics - Scanning for topic
Analytics - topic: Some(stock market)
Result: stock market
```

```scala
def run =
  topicOfInterestZ:
    "boring and inane content"
```

Output:

```shell
Analytics - Scanning for topic
Analytics - topic: None
Result: NoInterestingTopic()
```

## Either

- Execution is not deferred
- Cannot interrupt the code that is producing these values

We have an existing function `wikiArticle` that checks for articles on a topic:

```scala
val wikiResult: Either[
  NoWikiArticleAvailable,
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
```

Output:

```shell
Wiki - articleFor(stock market)
Result: detailed history of stock market
```

```scala
def run =
  wikiArticleZ:
    "barn"
```

Output:

```shell
Wiki - articleFor(barn)
Result: NoWikiArticleAvailable()
```

## AutoCloseable

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
```

Output:

```shell
File - OPEN
File - contains(topicOfInterest)
File - CLOSE
Result: false
```

Now we highlight the difference between the static scoping of `Using` or `ZIO.fromAutoCloseable`.

```scala
import scala.util.Using
import java.io.FileReader

def run =
  defer:
    Using(openFile("file1.txt")) {
      file1 =>
        Using(openFile("file2.txt")) {
          file2 =>
            file1.sameContent(file2)
        }
    }
```

Output:

```shell
File - OPEN
File - OPEN
side-effect print: comparing content
File - CLOSE
File - CLOSE
Result: Success(Success(true))
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
```

Output:

```shell
File - OPEN
File - OPEN
side-effect print: comparing content
File - CLOSE
File - CLOSE
Result: true
```

Our code remains flat.

## Try

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
      _ => DiskFull()
```

```scala
def run =
  defer:
    val file =
      openFileZ("file1").run
    writeToFileZ(file, "New data on topic").run
```

Output:

```shell
File - OPEN
File - write: New data on topic
File - CLOSE
Result: New data on topic
```

## Functions that throw

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

## Slow, blocking functions

TODO Decide example functionality

- AI analysis of news content?

TODO Prose about the long-running AI process here


```scala
val summaryTmp: String =
  summarize("topic")
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
    .timeoutFail(AITooSlow())(50.millis)
```

- We can't indicate if they block or not
- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments

## Sequencing

Another term for this form of composition is called `andThen` in Scala.

With ZIO you can use `zio-direct` to compose ZIOs sequentially with:


```scala
def run =
  defer:
    val topStory =
      findTopNewsStory.run
    textAlert(topStory).run
```

Output:

```shell
Texting story: Battery Breakthrough
```

### Short-circuiting

Short-circuiting is an essential part Effect Systems because they enable a linear sequence of expressions which helps make code much easier to understand.
The explicit knowledge of exactly how each Effect can fail is part of definition of the Effect.

In order for Effect Systems to have recovery operations, they must know when failure happens.

## Final Collective Criticism

{{ TODO: better subhead name }}

Each of original approaches gives you benefits, but you can't easily assemble a program that utilizes all of them.
They must be manually transformed into each other.

Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, and not easily changed.

The number of combinations is something like:
  PairsIn(numberOfConcepts)

## Fully Composed

Now that we have all of these well-defined effects, we can wield them in any combination and sequence we desire.

```scala
val researchHeadline =
  defer:
    val headline: String =
      getHeadlineZ().run

    val topic: String =
      topicOfInterestZ(headline).run

    val summaryFile: File =
      // TODO Use Scenario to determine file?
      openFileZ("file1.txt").run

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
override val bootstrap = headlineNotAvailable

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Result: HeadlineNotAvailable
```

```scala
override val bootstrap = noInterestingTopic

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: None
Result: NoInterestingTopic()
```

```scala
override val bootstrap = summaryReadThrows

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(unicode)
File - OPEN
File - contains(unicode)
File - summaryFor(unicode)
File - CLOSE
Result: NoSummaryAvailable(unicode)
```

```scala
override val bootstrap = noWikiArticleAvailable

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(barn)
File - OPEN
File - contains(barn)
Wiki - articleFor(barn)
File - CLOSE
Result: NoWikiArticleAvailable()
```

```scala
override val bootstrap = aiTooSlow

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(space)
File - OPEN
File - contains(space)
Wiki - articleFor(space)
AI - summarize - start
AI **INTERRUPTED**
File - CLOSE
Result: AITooSlow()
```

```scala
// TODO This inconsistently works. It frequently reports the AI problem again.
override val bootstrap = diskFull

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(genome)
File - OPEN
File - contains(genome)
Wiki - articleFor(genome)
AI - summarize - start
AI - summarize - end
File - disk full!
File - CLOSE
Result: DiskFull()
```

And finally, we see the longest, successful pathway through our application:

```scala
override val bootstrap = stockMarketHeadline

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(stock market)
File - OPEN
File - contains(stock market)
Wiki - articleFor(stock market)
AI - summarize - start
AI - summarize - end
File - write: market is not rational
File - CLOSE
Result: market is not rational
```

## Effects are Values

{{ TODO: enables, reuse, repeats, delays, etc }}

```scala
def run =
  defer:
    researchHeadline.run
    researchHeadline.run
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(stock market)
File - OPEN
File - contains(stock market)
Wiki - articleFor(stock market)
AI - summarize - start
AI - summarize - end
File - CLOSE
Result: AITooSlow()
```

```scala
def run =
  researchHeadline.repeatN(2)
```

Output:

```shell
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(stock market)
File - OPEN
File - contains(stock market)
Wiki - articleFor(stock market)
AI - summarize - start
AI - summarize - end
File - write: market is not rational
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(stock market)
File - OPEN
File - contains(stock market)
Wiki - articleFor(stock market)
AI - summarize - start
AI - summarize - end
File - write: market is not rational
Network - Getting headline
Analytics - Scanning for topic
Analytics - topic: Some(stock market)
File - OPEN
File - contains(stock market)
Wiki - articleFor(stock market)
AI - summarize - start
AI - summarize - end
File - write: market is not rational
File - CLOSE
File - CLOSE
File - CLOSE
Result: market is not rational
```

Repeating is a form of composability, because you are composing a program with itself
