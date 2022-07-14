package zio_intro

import zio.{Clock, ZIO, ZIOAppDefault, System}
import zio.Console.{readLine, printLine}

object HelloWorld extends ZIOAppDefault:
  def run = printLine("Hello World")

object FirstMeaningfulExample extends ZIOAppDefault:
  def run =
    for
      _    <- printLine("Give us your name:")
      name <- readLine
      _    <- printLine(s"$name")
    yield ()