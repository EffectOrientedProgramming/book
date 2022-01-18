package zioBasics

import java.io
import zio._
import zio.Console._
import zio.Console
import java.io.IOException
import console.FakeConsole

object RuntimeEx:
// This object's primary function is to
  // interpret/run other ZIO objects.
// In legacy code, it may be better to run
  // effects with a runtime object to preserve
  // the structure of the program.
// Using a runtime object allows for ZIO to be
  // run when ever and where ever.

  val runtime         = Runtime.default
  val exZio: UIO[Int] = ZIO.succeed(1)

  val exZio2: ZIO[Console, IOException, String] =
    for
      _    <- printLine("Input Word: ")
      word <- readLine
    yield word

  def displayWord(word: String) =
    println(s"Chosen Word: ${word}")

  @main
  def runZIO() =
    // Runtime excecutes the effects, and returns
    // their output value.
    println(runtime.unsafeRun(exZio))

    // Runtimes can be used in a function
    // parameter:
    displayWord(
      runtime.unsafeRun(
        exZio2.provide(
          ZLayer.succeed(FakeConsole.word)
        )
      )
    )
end RuntimeEx
