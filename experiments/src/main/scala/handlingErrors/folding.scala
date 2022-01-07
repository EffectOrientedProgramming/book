package handlingErrors

import zio.*
import zio.Console.*
import handlingErrors.file
import handlingErrors.standIn

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
    val message =
      logic.fold(
        _ => "The file wouldn't open... ",
        _ => "The file opened!"
      ) // Non-effectful handling

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
