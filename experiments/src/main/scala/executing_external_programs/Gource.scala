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
object Gource extends ZIOAppDefault {


  def run =
    Command("gource",
//      "--follow-user", "bfrasure", // Highlights user, but still shows others
      "--user-show-filter", "bfrasure", // Only shows user
      "/Users/bfrasure/Repositories/book"
    ).run
}
