package executing_external_programs

import zio._
import zio.Console.printLine
import zio.process.{Command, ProcessInput, ProcessOutput}

/*

   Possibilities:
    - Show a certain time period
      - More recent activity
    - Cycle between different repositories
*/
object GourceDemo extends ZIOAppDefault {

  def gource(repoDir: String) = Command("gource",
//      "--follow-user", "bfrasure", // Highlights user, but still shows others
      "--user-show-filter", "bfrasure|Bill Frasure", // Only shows user
      repoDir
    )

  val projects =
    List(
      "/Users/bfrasure/Repositories/book",
      "/Users/bfrasure/Repositories/TestFrameworkComparison",
    )

  def showActivityForAWhile(repoDir: String) =
    for {
      run1 <- gource(repoDir).run
      _ <- ZIO.sleep(5.seconds)
      _ <- run1.killForcibly
    } yield ()

  def randomProjectActivity =
    for {
      idx <- Random.nextIntBounded(projects.length)
      _ <- showActivityForAWhile(projects(idx))
    } yield ()
  def run =
    for {
      _ <- randomProjectActivity.repeatN(2)
    } yield ()
}
