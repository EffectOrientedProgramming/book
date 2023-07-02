package zio_intro

import zio.{Clock, ZIO, ZIOAppDefault, System}
import zio.Console.{readLine, printLine}
import zio.direct.*

object PromptUserForName extends ZIOAppDefault:
  def run =
    defer {
      printLine("Give us your name:").run
      val name = readLine.run
      printLine(s"Hello $name").run
    }
