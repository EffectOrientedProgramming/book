## Console Testing 

```scala 3
// TODO Either find a better home for this, or delete.
```

Consider a `Console` application:

```scala 3 mdoc:silent
import zio.*
import zio.direct.*

val logic =
  defer:
    println("a")
    val username =
      Console
        .readLine:
          "Enter your name\n"
        .run
    println("B")
    Console
      .printLine:
        s"Hello $username"
      .run
  .orDie
```

If we try to run this code in the same way as most of the examples in this book, we encounter a problem.

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

object HelloWorldWithTimeout
    extends zio.ZIOAppDefault:
  def run =
    logic.timeout(1.second)
```

We cannot execute this code and render the results for the book because it requires interaction with a user.
However, even if you are not trying to write demo code for a book, it is very limiting to need a user at the keyboard for your program to execute.
Even for the smallest programs, it is slow, error-prone, and boring.

```scala 3 mdoc:testzio
// TODO This is blowing up.
import zio.*
import zio.direct.*
import zio.test.*

def spec =
  test("console works"):
    defer:
      TestConsole
        .feedLines:
          "Zeb"
        .run

      logic.run

      val capturedOutput: String =
        TestConsole.output.run.mkString
      val expectedOutput =
        s"""|Enter your name
            |Hello Zeb
            |""".stripMargin
      assertTrue:
        capturedOutput == expectedOutput
```
