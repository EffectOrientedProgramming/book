## executing_external_programs

 

### experiments/src/main/scala/executing_external_programs/Gource.scala
```scala
package executing_external_programs

import zio.process.Command

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
    defer {
      val run1 = gource(repoDir).run.run
      ZIO.sleep(5.seconds).run
      run1.killForcibly.run
    }

  def randomProjectActivity =
    defer {
      val idx =
        Random
          .nextIntBounded(projects.length)
          .run
      showActivityForAWhile(projects(idx)).run
    }
  def run = randomProjectActivity.repeatN(2)

end GourceDemo

```


### experiments/src/main/scala/executing_external_programs/Say.scala
```scala
package executing_external_programs

import zio.process.Command

def say(message: String) =
  Command("say", message)

object SayDemo extends ZIOAppDefault:
  def run = say("Hello, world!").run

```


