package ZIOFromNothing

class XEnvironment():
  def increment(y: Int): Int =
    XEnvironment.x += y
    XEnvironment.x

object XEnvironment:
  private var x: Int = 0

case class IO(behavior: () => Unit):
  def compose(other: IO) =
    IO(() =>
      behavior()
      println("New behavior from compose")
      other.behavior()
    )

object Interpreter:
  def run(io: IO) = io.behavior()

@main
def runEffects =
  val hi    = IO(() => println("hi "))
  val there = IO(() => println("there!"))

  val fullApp = hi.compose(there)

  Interpreter.run(fullApp)

case class XIO[A](behavior: (x: String) => A):
  def compose(other: XIO[A]) =
    XIO[A]((x: String) =>
      println(s"Executing with Environment: $x")
      behavior(x)
      other.behavior(x)
    )

object XInterpreter:
  def run[A](io: XIO[A], x: String) =
    io.behavior(x)

@main
def XrunEffects =
  val hi    = XIO(x => println("hi "))
  val there = XIO(x => println("there!"))

  val x       = "Planet Z"
  val fullApp = hi.compose(there)

  XInterpreter.run(fullApp, x)

// 3

import zio._
import zio.Console.printLine

val zioLogic =
  for
    _ <- printLine("Accessing the environment")
    state1 <-
      ZIO.serviceWithZIO[XEnvironment](env =>
        ZIO.succeed(env.increment(1))
      )
    _ <- printLine("state1: " + state1)
    state2 <-
      ZIO.serviceWithZIO[XEnvironment](env =>
        ZIO.succeed(env.increment(1))
      )
    _ <- printLine("state2: " + state2)
  yield ()
object RealZIOEnvironmentPassingExplicitlyProvided
    extends ZIOAppDefault:
  def run =
    zioLogic.provideLayer(
      ZLayer.succeed(XEnvironment())
    )

object RealZIOEnvironmentPassingProvidingSome
    extends ZIOAppDefault:
  def run =
    zioLogic.provideSomeLayer(
      ZLayer.succeed(XEnvironment())
    )

case class XIO3[X, A](behavior: (x: X) => A):
  def compose(other: XIO3[X, A]) =
    XIO3[X, A]((x: X) =>
      behavior(x)
      println("New behavior from compose")
      other.behavior(x)
    )

object XIO3Interpreter:
  def run[X, A](io: XIO3[X, A], x: X) =
    io.behavior(x)

@main
def XIO3runEffects =
  val hi      = XIO3(x => println("hi "))
  val there   = XIO3(x => println("there!"))
  val fullApp = hi.compose(there)

  val z = "Planet Z"
  XIO3Interpreter.run(fullApp, z)

  val magicNumber = "42"
  XIO3Interpreter.run(fullApp, magicNumber)
