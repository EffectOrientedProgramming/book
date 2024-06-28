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

We will utilize several pre-defined functions to highlight less-complete effect alternatives.
The implementations are deliberately hidden to highlight the surprising nature of executing Effects and maintain focus on composability.

## Universal Composability


ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.


## Future

```scala
import scala.concurrent.Future
```

The original asynchronous datatype in Scala has several undesirable characteristics:

- Start executing immediately
- Cleanup is not guaranteed
- Must all fail with Exception
- Needs `ExecutionContext`s passed everywhere

There is a function that returns a Future:

```scala
val future: Future[String] =
  getHeadLine()
```

By wrapping this in `ZIO.from`, it will:

- Defer execution
- Let us attach finalizer behavior
- Let us customize the error type
- get the `ExecutionContext` it needs

```scala
def getHeadlineZ() =
  ZIO
    .from:
      getHeadLine()
    .orElseFail:
      HeadlineNotAvailable
```

```scala
override val bootstrap =
  stockMarketHeadline

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
override val bootstrap =
  headlineNotAvailable

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
For a more thorough discussion of this, see the [ZIO documentation](https://www.zio.dev/reference/resource/scope/).

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
    Using(openFile("file1.txt")):
      file1 =>
        Using(openFile("file2.txt")):
          file2 =>
            println:
              file1.sameContent(file2)
    () // Don't care about result
```

Output:

```shell
File - OPEN
File - OPEN
side-effect print: comparing content
true
File - CLOSE
File - CLOSE
```

With each new file we open, we have to nest our code deeper.

```scala
def run =
  defer:
    val file1 =
      openFileZ("file1.txt").run
    val file2 =
      openFileZ("file2.txt").run
    Console
      .printLine:
        file1.sameContent(file2)
      .run
```

Output:

```shell
File - OPEN
File - OPEN
side-effect print: comparing content
true
File - CLOSE
File - CLOSE
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
def writeToFileZ(
    file: File,
    content: String
) =
  ZIO
    .from:
      file.write:
        content
    .orElseFail:
      DiskFull()
```

```scala
def run =
  defer:
    val file =
      openFileZ("file1").run
    writeToFileZ(file, "New data on topic")
      .run
```

Output:

```shell
File - OPEN
File - write: New data on topic
File - CLOSE
Result: New data on topic
```

## Functions that throw

We covered the deficiencies of throwing functions in the previous chapter, so we will not belabor the point here.
We still want to show how they can be converted to Effects and cleanly fit into our composability story.

```scala
openFile("file1").summaryFor("space")
```

```scala
openFile("file1").summaryFor("unicode")
// java.lang.Exception: No summary available for unicode
// 	at repl.MdocSession$MdocApp$$anon$26.summaryFor(<input>:344)
// 	at repl.MdocSession$MdocApp.$init$$$anonfun$1(<input>:493)
```

```scala
case class NoSummaryAvailable(topic: String)

def summaryForZ(file: File, topic: String) =
  ZIO
    .attempt:
      file.summaryFor(topic)
    .orElseFail:
      NoSummaryAvailable(topic)
```

## Slow, blocking functions

Most of our examples in this chapter have specific failure behaviors that we handle.
However, we must also consider functions that are simply too slow.
Up to a point, latency is just the normal cost of doing business, but eventually it becomes unacceptable. 

Here, we are using a local Large Language Model to summarize content.
It does not have the same failure modes as the other functions, but its performance varies wildly.


```scala
val summaryTmp: String =
  summarize("long article")
```

This function is blocking, although it is not obvious from the signature.
This brings several downsides:

- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments

```scala
def summarizeZ(article: String) =
  ZIO
    .attemptBlockingInterrupt:
      summarize(article)
    .orDie
    .onInterrupt:
      ZIO.debug("AI **INTERRUPTED**")
    .timeoutFail(AITooSlow()):
      4000.millis
```

Now we have a way to confine the impact that this function has on our application.
Long-running invocations will be interrupted, although `attemptBlockingInterrupt` comes with a performance cost.
Carefully consider the trade-offs when using this function.

## Final Collective Criticism

```scala

```

Output:

```shell
TODO: better subhead name - Composed Pain? Compound Fracture?
```

Each of original approaches gives you benefits, but you can't easily assemble a program that utilizes all of them.
They must be manually transformed into each other.

Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, and not easily changed.

### Short-circuiting

Short-circuiting is an essential part of a user-friendly Effect Systems.
They enable a linear sequence of expressions which helps make code much easier to understand.
The explicit knowledge of exactly how each Effect can fail is part of definition of the Effect.

In order for Effect Systems to have recovery operations, they must know when failure happens.

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
      openFileZ("summaries.txt").run

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

```

Output:

```shell
TODO: Emphasize just how important this example is.
```

We now step through all the possible scenarios that can occur in our application.

### Headline Not Available

```scala
override val bootstrap =
  headlineNotAvailable

def run =
  researchHeadline
```

Output:

```shell
Network - Getting headline
Result: HeadlineNotAvailable
```

### No Interesting Topic In Headline

```scala
override val bootstrap =
  noInterestingTopic

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

### Exception when reading from file

```scala
override val bootstrap =
  summaryReadThrows

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

### No Wiki Article Available

```scala
override val bootstrap =
  noWikiArticleAvailable

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

### AI Too Slow

```scala
override val bootstrap =
  aiTooSlow

def run =
  researchHeadline
```

Output:

```shell
TODO: make sure onInterrupt debug shows up
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

### Disk Full

```scala
override val bootstrap =
  diskFull

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
File - CLOSE
Result: AITooSlow()
```

### Happy Path

And finally, we see the longest, successful pathway through our application:

```scala
override val bootstrap =
  stockMarketHeadline

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

```scala
// TODO: enables, reuse, repeats, delays, etc
override val bootstrap =
  stockMarketHeadline

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
Result: market is not rational
```

```scala
override val bootstrap =
  stockMarketHeadline

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