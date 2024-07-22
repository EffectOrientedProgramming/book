package manuscript

import zio.*
import zio.direct.*
import zio.test.*

object ManuscriptSpec extends ZIOSpecDefault{

  val wd =
      os.pwd / "manuscript"
  val chapters = os.list(wd)

  val composabilityChapter =
    chapters.find(_.toString.contains("Composability"))
      .getOrElse(throw new IllegalStateException("Composability chapter not found"))

  val forbiddenWords = List(
    "monad",
    "map",
    "flatmap",
    "applicative",
    "monoid",
    "typeclass",
    "functor",
    "category theory",
    "combinator",
    "fiber",
    "fork",
    "fold",
  ).map(_.toLowerCase)


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
      },
      test("ensure no long lines") {

        defer:
          assertTrue(
            ! chapters.exists(path =>
              os.read.lines(path).exists(line =>
                line.trim == "TODO Handle long line"
              )
            )
          )
      },
      test("ensure we always say 'compiler error' instead of 'compile error'") {

        defer:
          assertTrue(
            ! chapters.exists(path =>
              os.read.lines(path).exists(line =>
                line.toLowerCase.contains("compile error")
              )
            )
          )
      },
      test("no round quotes") {
        // TODO this test could be better for error reporting
        val roundQuotes = Set("’", "‘", "“", "”")
        defer:
          assertTrue(
            ! chapters.exists(path =>
              os.read.lines(path).exists(line =>
                roundQuotes.exists(line.contains)
              )
            )
          )
      },
      test("no forbidden words") {
        // TODO could match on whole words only or use regex or something more specialized
        val forbiddenWords = Set(
          "monad",
          "map",
          "flatmap",
          "applicative",
          "monoid",
          "typeclass",
          "functor",
          "category theory",
          "combinator",
          "fiber",
          "fork",
          "fold",
        ).map(_.toLowerCase)

        defer:
          assertTrue:
            !chapters.exists:
              path =>
                os.read.lines(path).exists:
                  line =>
                    forbiddenWords.exists:
                      line.toLowerCase.contains
      },

      suite("no forbidden words") (
        // TODO could match on whole words only or use regex or something more specialized
        (forbiddenWords.map:
          word =>
            test(word) {
              defer:
                assertTrue(
                  ! chapters.exists:
                    path =>
                      os.read.lines(path).exists:
                        line =>
                          line.toLowerCase.contains(word)
                )
            })*
      ),
      test("no deep indentation") {
        defer:
          assertTrue(
            ! chapters.exists(path =>
              os.read.lines(path).exists(line =>
                line.startsWith("              ")
              )
            )
          )
      },
    )

}
