package handlingErrors

import zio.*
import zio.console.*
import handlingErrors.file
import java.io.IOException

def standin
    : ZIO[console.Console, IOException, Unit] =
  putStrLn("Im a standin")

object catching extends zio.App:

  val logic = loadFile("TargetFile")

  def run(args: List[String]) =
    logic
      .catchAll(_ =>
        putStrLn("Error Cought")
        loadBackupFile()
      )
      .exitCode
end catching

// standin.exitCode
