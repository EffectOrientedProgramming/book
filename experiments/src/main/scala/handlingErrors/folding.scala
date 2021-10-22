package handlingErrors

import zio.*
import zio.Console.*
import handlingErrors.file
import handlingErrors.standin

object folding extends App:
// When applied to ZIO, fold() allows the
  // programmer to handle both failure
// and success at the same time.
// Zio's fold method can be broken into two
  // pieces: fold(), and foldM()
// fold() supplied a non-effectful handler, why
  // foldM() applies an effectful handler.

  val logic = loadFile("targetFile")

  def run(args: List[String]) =
    val message =
      logic.fold(
        _ => "The file wouldn't open... ",
        _ => "The file opened!"
      ) //Non-effectful handling

    logic
      .foldZIO(
        _ => loadBackupFile(),
        _ =>
          printLine(
            "The file opened on first attempt!"
          )
      ) //Effectful handling
      .exitCode
end folding
