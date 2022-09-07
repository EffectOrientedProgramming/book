package executing_external_programs

import zio.process.{Command, ProcessInput, ProcessOutput}
import zio._

def say(message: String) =
  Command("say", message)

object SayDemo extends ZIOAppDefault:
  def run =
    say("Hello, world!").run
