package handlingErrors

import zio.*
import zio.Console.*
import handlingErrors.file
import java.io.IOException

def standIn: ZIO[Console, IOException, Unit] =
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
