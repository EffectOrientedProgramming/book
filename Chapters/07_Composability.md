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

- Failures
- Async
- Blocking
- Managed resource
- Cancellation
- Either-ness
- Environmental requirements

These concepts and their competing solutions will be expanded on and contrasted with ZIO throughout this chapter.

We will utilize several pre-defined functions to highlight less-complete Effect alternatives.
The implementations are deliberately hidden to highlight the surprising nature of executing Effects and maintain focus on composability.

## Universal Composability

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

enum Scenario:
  case StockMarketHeadline
  case HeadlineNotAvailable
  case NoInterestingTopic()
  // There is an
  // Either[NoWikiArticleAvailable,_]
  // in visible code, so if we make it an
  // object,
  // It will be
  // Either[NoWikiArticleAvailable.type,_] :(
  case NoWikiArticleAvailable()
  case AITooSlow()
  case SummaryReadThrows()
  case DiskFull()

import Scenario.*

// the scenario is used from non-ZIO code, so we don't use the config / bootstrap approach to passing it.
// but we do still use bootstrap to set the scenario, just for consistency with how the scenario is set in other chapters
var scenario: Scenario =
  StockMarketHeadline

def stockMarketHeadline =
  scenario =
    StockMarketHeadline
  ZLayer.empty

def headlineNotAvailable =
  scenario =
    HeadlineNotAvailable
  ZLayer.empty

def noInterestingTopic =
  scenario =
    NoInterestingTopic()
  ZLayer.empty

def summaryReadThrows =
  scenario =
    SummaryReadThrows()
  ZLayer.empty

def noWikiArticleAvailable =
  scenario =
    NoWikiArticleAvailable()
  ZLayer.empty

def aiTooSlow =
  scenario =
    AITooSlow()
  ZLayer.empty

def diskFull =
  scenario =
    DiskFull()
  ZLayer.empty
```

ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

import scala.concurrent.Future
def getHeadLine(): Future[String] =
  println("Network - Getting headline")
  scenario match
    case Scenario.HeadlineNotAvailable =>
      Future.failed:
        new Exception(
          "Headline not available"
        )
    case Scenario.StockMarketHeadline =>
      Future
        .successful("stock market rising!")
    case Scenario.NoWikiArticleAvailable() =>
      Future.successful("Fred built a barn.")
    case Scenario.AITooSlow() =>
      Future.successful("space is big!")
    case Scenario.SummaryReadThrows() =>
      Future
        .successful("new unicode released!")
    case Scenario.NoInterestingTopic() =>
      Future.successful("boring content")
    case Scenario.DiskFull() =>
      Future
        .successful("human genome sequenced")
  end match
end getHeadLine

def findTopicOfInterest(
    content: String
): Option[String] =
  println("Analytics - Scanning for topic")
  val topics =
    List(
      "stock market",
      "space",
      "barn",
      "unicode",
      "genome"
    )
  val res =
    topics.find(content.contains)
  println(s"Analytics - topic: $res")
  res

import scala.util.Either
def wikiArticle(topic: String): Either[
  Scenario.NoWikiArticleAvailable,
  String
] =
  println(s"Wiki - articleFor($topic)")
  topic match
    case "stock market" | "space" |
        "genome" =>
      Right:
        s"detailed history of $topic"

    case "barn" =>
      Left:
        Scenario.NoWikiArticleAvailable()
```

## Future

```scala 3 mdoc
import zio.*
import zio.direct.*

import scala.concurrent.Future
```

The original asynchronous datatype in Scala has several undesirable characteristics:

- Start executing immediately
- Cleanup is not guaranteed
- Must all fail with Exception
- Needs `ExecutionContext`s passed everywhere

There is a function that returns a Future:

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

val future: Future[String] =
  getHeadLine()
```

By wrapping this in `ZIO.from`, it will:

- Defer execution
- Let us attach finalizer behavior
- Let us customize the failure type
- get the `ExecutionContext` it needs

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

def getHeadlineZ() =
  ZIO
    .from:
      getHeadLine()
    .orElseFail:
      HeadlineNotAvailable
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  stockMarketHeadline

def run =
  getHeadlineZ()
```

Now let's confirm the behavior when the headline is not available.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  headlineNotAvailable

def run =
  getHeadlineZ()
```

## Option

`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, failure types, or anything else.
It merely indicates that a value might not be available.

- Execution is not deferred
- Cannot interrupt the code that is producing these values

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val result: Option[String] =
  findTopicOfInterest:
    "content"
```

If you want to treat the case of a missing value as a failure, you can again use `ZIO.from`:
ZIO will convert `None` into a generic failure, giving you the opportunity to define a more specific type.

```scala 3 mdoc
import zio.*
import zio.direct.*

def topicOfInterestZ(headline: String) =
  ZIO
    .from:
      findTopicOfInterest:
        headline
    .orElseFail:
      NoInterestingTopic()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  topicOfInterestZ:
    "stock market rising!"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  topicOfInterestZ:
    "boring and inane content"
```

## Either

- Execution is not deferred
- Cannot interrupt the code that is producing these values

We have an existing function `wikiArticle` that checks for articles on a topic:

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

val wikiResult: Either[
  NoWikiArticleAvailable,
  String
] =
  wikiArticle("stock market")
```

```scala 3 mdoc
import zio.*
import zio.direct.*

def wikiArticleZ(topic: String) =
  ZIO.from:
    wikiArticle:
      topic
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  wikiArticleZ:
    "stock market"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  wikiArticleZ:
    "barn"
```

## AutoCloseable

Java/Scala provide the `AutoCloseable` interface for defining finalizer behavior on objects.
While this is a big improvement over manually managing this in ad-hoc ways, the static scoping of this mechanism makes it clunky to use.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

import scala.util.Try

trait File extends AutoCloseable:
  def contains(searchTerm: String): Boolean
  def write(entry: String): Try[String]
  def summaryFor(searchTerm: String): String
  def sameContent(other: File): Boolean
  def content(): String

def openFile(path: String) =
  new File:
    var contents: List[String] =
      List("Medical Breakthrough!")
    println("File - OPEN")

    override def content() =
      path match
        case "file1.txt" | "file2.txt" |
            "summaries.txt" =>
          "hot dog"
        case _ =>
          "not hot dog"

    override def sameContent(
        other: File
    ): Boolean =
      println(
        "side-effect print: comparing content"
      )
      content() == other.content()

    override def close =
      println("File - CLOSE")

    override def contains(
        searchTerm: String
    ): Boolean =

      val result =
        searchTerm match
          case "wheel" | "unicode" =>
            true
          case _ =>
            false
      println:
        s"File - contains($searchTerm) => $result"
      result

    override def summaryFor(
        searchTerm: String
    ): String =
      println(
        s"File - summaryFor($searchTerm)"
      )
      if (searchTerm == "unicode")
        println("File - * Threw Exception *")
        throw Exception(s"No summary found")
      else if (searchTerm == "stock market")
        "stock markets are neat"
      else if (searchTerm == "space")
        "space is huge"
      else
        ???

    override def write(
        entry: String
    ): Try[String] =
      if (entry.contains("genome")) {
        println("File - disk full!")
        Try(
          throw new Exception(
            "Disk is full!"
          )
        )
      } else {
        println("File - write: " + entry)
        contents =
          entry :: contents
        Try(entry)
      }
```

We have an existing function that produces an `AutoCloseable`.

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

val file: AutoCloseable =
  openFile("file1")
```

Since `AutoCloseable` is a trait that can be implemented by arbitrary classes, we can't rely on `ZIO.from` to automatically manage this conversion for us.
In this situation, we should use the explicit `ZIO.fromAutoCloseable` function.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

def openFileZ(path: String) =
  ZIO.fromAutoCloseable:
    ZIO.succeed:
      openFile(path)
```

Once we do this, the `ZIO` runtime will manage the lifecycle of this object via the `Scope` mechanism.
For a more thorough discussion of this, see the [ZIO documentation](https://www.zio.dev/reference/resource/scope/).

Now we open a `File`, and check if it contains a topic of interest.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    val file =
      openFileZ("file1.txt").run
    file.contains:
      "topicOfInterest"
```

Now we highlight the difference between the static scoping of `Using` or `ZIO.fromAutoCloseable`.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

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

With each new file we open, we have to nest our code deeper.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*
import zio.Console.*

def run =
  defer:
    val file1 =
      openFileZ("file1.txt").run
    val file2 =
      openFileZ("file2.txt").run
    printLine:
      file1.sameContent(file2)
    .run
```

Our code remains flat.

## Try

Next we want to write to our `File`.
The existing API uses a `Try` to indicate success or failure.

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

val writeResult: Try[String] =
  openFile("file1").write("asdf")
```

```scala 3 mdoc
import zio.*
import zio.direct.*

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

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    val file =
      openFileZ("file1").run
    writeToFileZ(file, "New data on topic")
      .run
```

## Functions that throw

We covered the deficiencies of throwing functions in the previous chapter, so we will not belabor the point here.
We still want to show how they can be converted to Effects and cleanly fit into our composability story.

```scala 3 mdoc:runzio
def run =
  defer:
    openFile("file1").summaryFor("space")
```

```scala 3 mdoc:runzio
def run =
  defer:
    openFile("file1").summaryFor("unicode")
```

```scala 3 mdoc
import zio.*
import zio.direct.*

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
However, we must also consider functions that are too slow.
Up to a point, latency is just the normal cost of doing business, but eventually it becomes unacceptable. 

Here, we are using a local Large Language Model to summarize content.
It does not have the same failure modes as the other functions, but its performance varies wildly.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

def summarize(article: String): String =
  println(s"AI - summarize - start")
  // Represents the AI taking a long time to
  // summarize the content
  if (article.contains("space")) {
    println("AI - taking a long time")
    Thread.sleep(5000)
  }

  println(s"AI - summarize - end")
  if (article.contains("stock market"))
    s"market is not rational"
  else if (article.contains("genome"))
    "The human genome is huge!"
  else if (article.contains("long article"))
    "short summary"
  else
    ???
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    summarize("long article")
```

This function is blocking, although it is not obvious from the signature.
This brings several downsides:

- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments

```scala 3 mdoc
import zio.*
import zio.direct.*

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

```scala 3 mdoc:runzio
def run =
  summarizeZ("long article")
```

Long-running invocations will be interrupted if they take too long.

```scala 3 mdoc:runzio
def run =
  summarizeZ("space")
```

However, `attemptBlockingInterrupt` comes with a performance cost.
Carefully consider the trade-offs between implementing an upper bound vs slowing down the average run when using this function.

## Losing your Composure

Each of the original approaches gives you benefits, but you can't easily assemble a program that utilizes all of them.
They must be manually transformed into each other.

Instead of the best of all worlds, you get the pain of all worlds.
eg `Closeable[Future[Either[Throwable, A]]]`
The ordering of the nesting is significant, confusing, and not easily changed.

## Fully Composed

Now that we have all of these well-defined Effects, we can wield them in any combination and sequence we desire.

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

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

We consider this sequence the most significant achievement of this book.
Without a powerful, general Effect type, you are constantly struggling to jump from 1 limited Effect to the next.
With ZIO, you can build real-word, complex applications that all flow cleanly into one supreme type.

We now step through all the possible scenarios that can occur in our application.

### Headline Not Available

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  headlineNotAvailable

def run =
  researchHeadline
```

### No Interesting Topic In Headline

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  noInterestingTopic

def run =
  researchHeadline
```

### Exception when reading from file

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  summaryReadThrows

def run =
  researchHeadline
```

### No Wiki Article Available

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  noWikiArticleAvailable

def run =
  researchHeadline
```

### AI Too Slow

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  aiTooSlow

def run =
  researchHeadline
```

### Disk Full

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap =
  diskFull

def run =
  researchHeadline
```

### Happy Path

And finally, we see the longest, successful pathway through our application:

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

override val bootstrap =
  stockMarketHeadline

def run =
  researchHeadline
```

## Effects are Values

We want to revisit and re-emphasize that Effects are values.
This can be difficult to remember when viewing and executing larger programs.
Representing a complex workflow as a value allows us to manipulate it in ways that are not possible with other approaches.

Suppose the requirements of the system change, and now you need to ensure that the whole process completes within a strict time limit.
Even though we already have a narrow timeout attached to the AI summarize call, we are still free to attach a more restrictive timeout.

```scala 3 mdoc:silent
val strictResearch =
  researchHeadline
    .timeoutFail("strict timeout"):
      1.millisecond
```

```scala 3 mdoc:runzio
override val bootstrap =
  stockMarketHeadline

def run =
  strictResearch
```
Repeating is a form of composability, because you are composing a program value with itself and a delay.
Now that we have a nice, single-shot workflow that will analyze the current headline, we can make it run every day.

```scala 3 mdoc:silent
val daily =
  strictResearch.repeat:
    Schedule.spaced(24.hours)
```
