package hello_failures

import zio.*
import zio.Console.*
import hello_failures.file
import java.io.IOException

def standIn: ZIO[Any, IOException, Unit] =
  printLine("Im a stand-in")

object catching extends zio.ZIOAppDefault:

  val logic = loadFile("TargetFile")

  def run =
    logic
      .catchAll(_ =>
        println("Error Caught")
        loadBackupFile()
      )
      .exitCode

// standIn.exitCode
