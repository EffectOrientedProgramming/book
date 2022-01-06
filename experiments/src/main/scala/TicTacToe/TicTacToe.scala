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

// -----------------------------------

abstract case class Name private (name: String)

object Name:
  def make(name: String) = new Name(name){}

// Can't inherit because constructor is private:
// class FirstName(fn: String) extends Name(fn)

def bar =
  val name: Name = Name.make("Bob")
  //  val name2 = new Name("Joe") {}
  //  val jan = name.copy("Janet")

// -----------------------------------

trait X
object X:
  var x: Int = 0

trait XIO[IO, R]

case class IntXIO(i: Int) extends XIO[X, Int]

def combine2(a: Int, b: Int): XIO[X, Int] =
  X.x += 1
  IntXIO(a + b + X.x)

def foo =
  combine2(1, 2)

//trait YIO[ENV, F, R] extends Either[F, R]

