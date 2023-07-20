package zio_intro

import zio.Console.{readLine, printLine}

object PromptUserForName extends ZIOAppDefault:
  def run =
    defer {
      printLine("Give us your name:").run
      val name = readLine.run
      printLine(s"Hello $name").run
    }
