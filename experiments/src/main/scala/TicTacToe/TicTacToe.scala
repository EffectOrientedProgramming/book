// From: https://scalac.io/blog/write-command-line-application-with-zio/
package TicTacToe
import zio.{Console, ZIOAppDefault , ZEnv, ZIO}

enum MenuCommand:
  case NewGame, Resume, Quit, Invalid


object TicTacToe extends ZIOAppDefault:

  val program: ZIO[Console, java.io.IOException, Unit] =
    Console.printLine("TicTacToe game!")

  def run =
    program.foldZIO(
      error => Console.printLine(s"Execution failed with: $error") *> ZIO.succeed(1)
      , _ => ZIO.succeed(0)
    )

abstract case class Name(name: String)
object Name
