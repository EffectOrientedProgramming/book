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

## Universal Composability

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

enum Scenario: // TODO Could these instances _also_ be the error types??
  case StockMarketHeadline
  case HeadlineNotAvailable
  case NoInterestingTopic()
  // There is an Either[NoWikiArticleAvailable,_]
  // in visible code, so if we make it an object,
  // It will be
  // Either[NoWikiArticleAvailable.type,_] :(
  case NoWikiArticleAvailable()
  case AITooSlow()
  case SummaryReadThrows()
  case DiskFull()

import Scenario.*

// the scenario is used from non-ZIO code, so we don't use the config / bootstrap approach to passing it.
// but we do still use bootstrap to set the scenario, just for consistency with how the scenario is set in other chapters
var scenario: Scenario = StockMarketHeadline

def stockMarketHeadline =
  scenario = StockMarketHeadline
  ZLayer.empty

def headlineNotAvailable =
  scenario = HeadlineNotAvailable
  ZLayer.empty

def noInterestingTopic =
  scenario = NoInterestingTopic()
  ZLayer.empty

def summaryReadThrows =
  scenario = SummaryReadThrows()
  ZLayer.empty

def noWikiArticleAvailable =
  scenario = NoWikiArticleAvailable()
  ZLayer.empty

def aiTooSlow =
  scenario = AITooSlow()
  ZLayer.empty

def diskFull =
  scenario = DiskFull()
  ZLayer.empty
```

ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

## Existing Code

{{ TODO: subhead rename and what else needs to be here? }}

We will utilize several pre-defined functions to highlight less-complete effect alternatives.

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

import scala.concurrent.Future
// TODO If we make this function accept the "mock" result and return that, then
//  we can leverage that to hit all of the possible paths in AllTheThings.
def getHeadLine(): Future[String] =
  println("Network - Getting headline")
  scenario match
    case Scenario.HeadlineNotAvailable =>
      Future.failed:
        new Exception("Headline not available")
    case Scenario.StockMarketHeadline =>
      Future.successful("stock market rising!")
    case Scenario.NoWikiArticleAvailable() =>
      Future.successful("Fred built a barn.")
    case Scenario.AITooSlow() =>
      Future.successful("space is big!")
    case Scenario.SummaryReadThrows() =>
      Future.successful("new unicode released!")
    case Scenario.NoInterestingTopic() =>
      Future.successful(
        "TODO Use boring content here"
      )
    case Scenario.DiskFull() =>
      Future.successful("human genome sequenced")
end getHeadLine

def findTopicOfInterest(
    content: String
): Option[String] =
  // TODO Decide best output string here
  println("Analytics - Scanning")
  val topics =
    List(
      "stock market",
      "space",
      "barn",
      "unicode",
      "genome"
    )
  topics.find(content.contains)

import scala.util.Either
def wikiArticle(topic: String): Either[
  Scenario.NoWikiArticleAvailable,
  String
] =
  println(s"Wiki - articleFor($topic)")
  topic match
    case "stock market" | "space" | "genome" =>
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

- Cleanup is not guaranteed
- Start executing immediately
- Must all fail with Exception
- Needs `ExecutionContext`s passed everywhere

There is a function that returns a Future:

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

val future: Future[String] =
  getHeadLine()
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

def getHeadlineZ() =
  ZIO
    .from:
      getHeadLine()
    .mapError:
      case _: Throwable =>
        HeadlineNotAvailable
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = stockMarketHeadline

def run =
  getHeadlineZ()
```

Now let's confirm the behavior when the headline is not available.

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = headlineNotAvailable

def run =
  getHeadlineZ()
```

## Option

`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, error types, or anything else.
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

If you want to treat the case of a missing value as an error, you can again use `ZIO.from`:
ZIO will convert `None` into a generic error type, giving you the opportunity to define a more specific error type.

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

TODO Decide whether to show nested files example to highlight this weakness

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

import scala.util.Try

// TODO Different name to make less confusable with AutoCloseable?
trait File extends AutoCloseable:
  // TODO Return existing entry, rather than a
  // raw Boolean?
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
        case "file1.txt" | "file2.txt" =>
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
      println:
        s"File - contains($searchTerm)"

      // todo use path to determine behavior?
      searchTerm match
        case "wheel" | "unicode" =>
          true
        case _ =>
          false

    override def summaryFor(
        searchTerm: String
    ): String =
      println(s"File - summaryFor($searchTerm)")
      if (searchTerm == "unicode")
        throw Exception(
          s"No summary available for $searchTerm"
        )
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
        Try(throw new Exception("Disk is full!"))
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
TODO Link to docs for this?

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

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

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

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    val file1 =
      openFileZ("file1.txt").run
    val file2 =
      openFileZ("file2.txt").run
    file1.sameContent(file2)
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

def writeToFileZ(file: File, content: String) =
  ZIO
    .from:
      file.write:
        content
    .mapError:
      _ => DiskFull()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    val file =
      openFileZ("file1").run
    writeToFileZ(file, "New data on topic").run
```

## Functions that throw

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

val summary: String =
  openFile("file1").summaryFor("asdf")
```

```scala 3 mdoc
import zio.*
import zio.direct.*

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

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

def summarize(article: String): String =
  println(s"AI - summarize - start")
  // Represents the AI taking a long time to
  // summarize the content
  if (article.contains("space"))
    // This should go away when our clock is less
    // dumb
    println(
      "printing because our test clock is insane"
    )
    Thread.sleep(1000)

  println(s"AI - summarize - end")
  if (article.contains("stock market"))
    s"market is not rational"
  else if (article.contains("genome"))
    "The human genome is huge!"
  else if (article.contains("topic"))
    "topic summary"
  else
    ???
end summarize
```

```scala 3 mdoc
import zio.*
import zio.direct.*

// TODO Can we use silent instead of compile-only above?
val summary: String =
  summarize("topic")
```

This gets interrupted, although it takes a big performance hit

```scala 3 mdoc
import zio.*
import zio.direct.*

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

```scala 3 mdoc:invisible
import zio.*
import zio.direct.*

val findTopNewsStory =
  ZIO.succeed:
    "Battery Breakthrough"

def textAlert(message: String) =
  Console.printLine:
    s"Texting story: $message"
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

def run =
  defer:
    val topStory =
      findTopNewsStory.run
    textAlert(topStory).run
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

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

def researchHeadline() =
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

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = headlineNotAvailable

def run =
  researchHeadline()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = noInterestingTopic

def run =
  researchHeadline()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = summaryReadThrows

def run =
  researchHeadline()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = noWikiArticleAvailable

def run =
  researchHeadline()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = aiTooSlow

def run =
  researchHeadline()
```

```scala 3 mdoc:runzio
import zio.*
import zio.direct.*

override val bootstrap = diskFull

def run =
  researchHeadline()
```

And finally, we see the longest, successful pathway through our application:

```scala 3 mdoc:runzio:liveclock
import zio.*
import zio.direct.*

override val bootstrap = stockMarketHeadline

def run =
  researchHeadline()
```

## Effects are Values

{{ TODO: enables, reuse, repeats, delays, etc }}

```scala 3 mdoc:fail
def run =
  researchHeadling.run
  researchHeadling.run
```

```scala 3 mdoc:fail
def run =
  researchHeadling.repeatN(2).run
```

Repeating is a form of composability, because you are composing a program with itself
