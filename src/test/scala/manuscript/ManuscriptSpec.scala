package manuscript

import zio.test.*
import zio.*

import os.up

object ManuscriptSpec extends ZIOSpecDefault{

  val wd =
    if (os.pwd.toString.contains("/book/book"))
      os.pwd/ up / "manuscript"
    else
      os.pwd / "manuscript"
  val chapters = os.list(wd)

  val composabilityChapter =
    chapters.find(_.toString.contains("Composability"))
      .getOrElse(throw new IllegalStateException("Composability chapter not found"))

  def spec =
    suite("ManuscriptSpec")(
      test("confirm that test timeout output is in manuscript") {
        defer:
          assertTrue(
            chapters.exists(path =>
              os.read.lines(path).exists(line =>
                line.trim == "Timeout of 1 s exceeded."
              )
            )
          )
      },
      test("confirm that onInterrupt output is in Composability chapter") {
        defer:
          assertTrue(
              os.read.lines(composabilityChapter).count(line =>
                line.trim == "AI **INTERRUPTED**"
              ) == 2
          )
      }
    )

}
