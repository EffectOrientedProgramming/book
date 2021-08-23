package handlingErrors

import zio.*
import zio.Console.*
import handlingErrors.file
import java.io.IOException

def standin
    : ZIO[Has[Console], IOException, Unit] =
  printLine("Im a standin")

object catching extends zio.App:

  val logic = loadFile("TargetFile")

  def run(args: List[String]) =
    logic
      .catchAll(_ =>
        println("Error Caught")
        loadBackupFile()
      )
      .exitCode

// standin.exitCode
