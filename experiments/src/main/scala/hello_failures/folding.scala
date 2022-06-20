package hello_failures

import zio.*
import zio.Console.*
import hello_failures.file
import hello_failures.standIn

object folding extends ZIOAppDefault:
// When applied to ZIO, fold() allows the
  // programmer to handle both failure
// and success at the same time.
// ZIO's fold method can be broken into two
  // pieces: fold(), and foldM()
// fold() supplied a non-effectful handler, why
  // foldM() applies an effectful handler.

  val logic = loadFile("targetFile")

  def run =
    logic
      .foldZIO(
        _ => loadBackupFile(),
        _ =>
          printLine(
            "The file opened on first attempt!"
          )
      ) // Effectful handling
      .exitCode
end folding
