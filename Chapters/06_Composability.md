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
enum Scenario: // TODO Could these instances _also_ be the error types??
  case StockMarketHeadline
  case HeadlineNotAvailable
  case NoInterestingTopic()
  // There is an Either[NoWikiArticleAvailable,_] in visible code, so if we make it an object,
  // It will be Either[NoWikiArticleAvailable.type,_] :(
  case NoWikiArticleAvailable()
  case AITooSlow()
  case SummaryReadThrows()
  case DiskFull()
```

ZIOs compose in a way that covers all of these concerns.
The methods for composability depend on the desired behavior.

When writing complex applications
  , you will encounter APIs that that return limited data types.
  
ZIO provides conversion methods that take these limited data types and turn them into its single, universally composable type.

### Existing Code

We will utilize several pre-defined functions to highlight less-complete effect alternatives.

```scala mdoc:invisible
import scala.concurrent.Future
// TODO If we make this function accept the "mock" result and return that, then
//  we can leverage that to hit all of the possible paths in AllTheThings.
def getHeadLine(
    scenario: Scenario
): Future[String] =
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
      Future.successful("TODO Use boring content here")
    case Scenario.DiskFull() =>
      Future.successful("human genome sequenced")

def findTopicOfInterest(
    content: String
): Option[String] =
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


### Future

```scala mdoc
import scala.concurrent.Future
```

The original asynchronous datatype in Scala has several undesirable characteristics:

- Cleanup is not guaranteed
- Start executing immediately
- Must all fail with Exception
- Needs `ExecutionContext`s passed everywhere


There is a function that returns a Future:

```scala mdoc:compile-only
val future: Future[String] = getHeadLine(???)
```

TODO This is repetitive after listing the downsides above.
By wrapping this in `ZIO.from`, it will:

- get the `ExecutionContext` it needs
- Defer execution of the code
- Let us attach finalizer behavior
- Give us the ability to customize the error type

```scala mdoc:silent
def getHeadlineZ(scenario: Scenario) =
  ZIO
    .from:
      getHeadLine(scenario)
    .mapError:
      case _: Throwable =>
        Scenario.HeadlineNotAvailable
```

```scala mdoc:runzio
def run =
  getHeadlineZ(Scenario.StockMarketHeadline)
```
Now let's confirm the behavior when the headline is not available.

```scala mdoc:runzio
def run =
  getHeadlineZ(Scenario.HeadlineNotAvailable)
```

### Option
`Option` is the simplest of the alternate types you will encounter.
It does not deal with asynchronicity, error types, or anything else.
It merely indicates that a value might not be available.

- Execution is not deferred
- Cannot interrupt the code that is producing these values


```scala mdoc:silent
val result: Option[String] =
  findTopicOfInterest:
    "content"
```

If you want to treat the case of a missing value as an error, you can again use `ZIO.from`:
ZIO will convert `None` into a generic error type, giving you the opportunity to define a more specific error type.

```scala mdoc
def topicOfInterestZ(headline: String) =
  ZIO
    .from:
      findTopicOfInterest:
        headline
    .orElseFail:
      Scenario.NoInterestingTopic()
```

```scala mdoc:runzio
def run =
  topicOfInterestZ:
    "stock market rising!"
```

```scala mdoc:runzio
def run =
  topicOfInterestZ:
    "boring and inane content"
```

### Either

- Execution is not deferred
- Cannot interrupt the code that is producing these values

We have an existing function `wikiArticle` that checks for articles on a topic:

```scala mdoc:compile-only
val wikiResult: Either[
  Scenario.NoWikiArticleAvailable,
  String
] =
  wikiArticle("stock market")
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
    "barn"
```

### AutoCloseable
Java/Scala provide the `AutoCloseable` interface for defining finalizer behavior on objects.
While this is a big improvement over manually managing this in ad-hoc ways, the static scoping of this mechanism makes it clunky to use.

TODO Decide whether to show nested files example to highlight this weakness

```scala mdoc:invisible
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
        case "file1.txt" | "file2.txt"=> "hot dog"
        case _ => "not hot dog"
    
    override def sameContent(other: File): Boolean =
      println("side-effect print: comparing content")
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
      if (entry.contains("genome"))
        Try(
          throw new Exception(
            "Stock market already exists!"
          )
        )
      else {
        println("File - write: " + entry)
        contents =
          entry :: contents
        Try(entry)
      }
```


We have an existing function that produces an `AutoCloseable`.

```scala mdoc:compile-only
val file: AutoCloseable =
  openFile("file1")
```

Since `AutoCloseable` is a trait that can be implemented by arbitrary classes, we can't rely on `ZIO.from` to automatically manage this conversion for us.
In this situation, we should use the explicit `ZIO.fromAutoCloseable` function.

```scala mdoc:silent
def openFileZ(path: String) =
  ZIO.fromAutoCloseable:
    ZIO.succeed:
      openFile(path)
```

Once we do this, the `ZIO` runtime will manage the lifecycle of this object via the `Scope` mechanism.
TODO Link to docs for this?

Now we open a `File`, and check if it contains a topic of interest.

```scala mdoc:runzio
def run =
  defer:
    val file =
      openFileZ("file1.txt").run
    file.contains:
      "topicOfInterest"
```

Now we highlight the difference between the static scoping of `Using` or `ZIO.fromAutoCloseable`.

```scala mdoc:compile-only
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


```scala mdoc:runzio
def run =
  defer:
    val file1 =
      openFileZ("file1.txt").run
    val file2 =
      openFileZ("file2.txt").run
    file1.sameContent(file2)
```

Our code remains flat.

### Try
Next we want to write to our `File`.
The existing API uses a `Try` to indicate success or failure.

```scala mdoc:compile-only
val writeResult: Try[String] =
  openFile("file1").write("asdf")
```

```scala mdoc
def writeToFileZ(file: File, content: String) =
  ZIO
    .from:
      file.write:
        content
    .mapError:
      _ => Scenario.DiskFull()
```

```scala mdoc:runzio
def run =
  defer:
    val file =
      openFileZ("file1").run
    writeToFileZ(file, "New data on topic").run
```



### Functions that throw

```scala mdoc:compile-only
openFile("file1").summaryFor("asdf"): String
```

```scala mdoc
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
```scala mdoc:invisible
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

```


```scala mdoc
// TODO Can we use silent instead of compile-only above?
val summary: String = summarize("topic")
```

This gets interrupted, although it takes a big performance hit
```scala mdoc
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

```scala mdoc:runzio
def run =
  researchHeadline:
    Scenario.HeadlineNotAvailable
```

```scala mdoc:runzio
def run =
  researchHeadline:
    Scenario.SummaryReadThrows()
```

```scala mdoc:runzio
def run =
  researchHeadline:
    Scenario.NoWikiArticleAvailable()
```

```scala mdoc:runzio
def run =
  researchHeadline:
    Scenario.AITooSlow()
```

```scala mdoc:runzio
def run =
  researchHeadline:
    // TODO Handle inconsistency in this example
    // AI keeps timing out
    Scenario.DiskFull()
```

And finally, we see the longest, successful pathway through our application:

```scala mdoc:runzio:liveclock
def run =
  researchHeadline:
    Scenario.StockMarketHeadline
```


## Repeats

Repeating is a form of composability, because you are composing a program with itself


## Injecting Behavior before/after/around


# Graveyard candidates

## Contract-based prose
Good contracts make good composability.

contracts are what makes composability work at scale
our effects put in place contracts on how things can compose


### Plain functions that return Unit TODO Incorporate to AllTheThings
{{TODO Decide if this section is worth keeping. If so, where?}}

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
