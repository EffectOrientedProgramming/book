package manuscript

import zio.test.*
import zio.*

object ManuscriptSpec extends ZIOSpecDefault{
  val wd = os.pwd / "manuscript"
  val chapters = os.list(wd)

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
      }
    )

}
