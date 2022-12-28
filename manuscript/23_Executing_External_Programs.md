# Executing External Programs
Most of this book focuses on executing Scala code all confined within a single JVM. 
However, there are times when you need to execute external programs. 
As a rule, we must treat these programs as side-effecting, because there is no practical way of ensuring they are pure.
We will explore this using `zio-process`
This chapter will cover how to do that.

## Basic shell tools
### Say
We could start with things like `echo` or `ls`, but those are easily done within Scala itself, so they are not very interesting.


### Top
### Git
## Advanced tools
### Gource
### Shpotify
## Running other programming languages
### Python
### Scala

## Automatically attached experiments.
 These are included at the end of this
 chapter because their package in the
 experiments directory matched the name
 of this chapter. Enjoy working on the
 code with full editor capabilities :D

 

### experiments/src/main/scala/executing_external_programs/Gource.scala
```scala
package executing_external_programs

import zio._
import zio.Console.printLine
import zio.process.{
  Command,
  ProcessInput,
  ProcessOutput
}

/* Possibilities:
 * - Show a certain time period
 * - More recent activity
 * - Cycle between different repositories */
object GourceDemo extends ZIOAppDefault:

  def gource(repoDir: String) =
    Command(
      "gource",
//      "--follow-user", "bfrasure", // Highlights user, but still shows others
      "--user-show-filter",
      "bfrasure|Bill Frasure", // Only shows user
      repoDir
    )

  val projects =
    List(
      "/Users/bfrasure/Repositories/book",
      "/Users/bfrasure/Repositories/TestFrameworkComparison"
    )

  def showActivityForAWhile(repoDir: String) =
    for
      run1 <- gource(repoDir).run
      _    <- ZIO.sleep(5.seconds)
      _    <- run1.killForcibly
    yield ()

  def randomProjectActivity =
    for
      idx <-
        Random.nextIntBounded(projects.length)
      _ <- showActivityForAWhile(projects(idx))
    yield ()
  def run =
    for _ <- randomProjectActivity.repeatN(2)
    yield ()
end GourceDemo

```


### experiments/src/main/scala/executing_external_programs/Say.scala
```scala
package executing_external_programs

import zio.process.{
  Command,
  ProcessInput,
  ProcessOutput
}
import zio._

def say(message: String) =
  Command("say", message)

object SayDemo extends ZIOAppDefault:
  def run = say("Hello, world!").run

```

            