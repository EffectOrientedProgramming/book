package helloworld

import scala.annotation.{experimental, nowarn}

@experimental
@nowarn
@zioMain
def run = Console.printLine("hello, * world")
