package executing_external_programs

import zio.*
import zio.process.Command

def say(message: String) =
  Command("say", message)

object SayDemo extends ZIOAppDefault:
  def run = say("Hello, world!").run
