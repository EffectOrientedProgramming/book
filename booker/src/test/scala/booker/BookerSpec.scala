package booker

import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.io.File

object BookerSpec extends ZIOSpecDefault:
  def nope(s: String): TestResult =
    zio
      .test
      .assert(parseChapter(File(s)))(isNone)

  def yuup(s: String, i: Int): TestResult =
    val f = File(s)
    zio
      .test
      .assert(parseChapter(f))(
        isSome(equalTo(i -> f))
      )

  def spec =
    suite("BookerSpec")(
      suite("parseChapter")(
        test("must start with a num") {
          nope("fooo.md") && nope("_foo.md") &&
          nope("0foo.md") &&
          yuup("0_foooo.md", 0) &&
          yuup("01_fooo.md", 1) &&
          yuup("12_foo.md", 12)
        },
        test("must end with .md") {
          nope("1_foooomd") &&
          nope("1_foo.txt") &&
          yuup("1_fooo.md", 1)
        }
      ),
//      suite("duplicates")(
//        test("must work") {
//          val in =
//            Seq(
//              File("1_foo.md"),
//              File("1_bar.md"),
//              File("2_baz.md")
//            )
//          val out =
//            Seq(
//              File("1_foo.md"),
//              File("1_bar.md")
//            )
// assert(duplicates(in))(equalTo(out))
//        }
//      ),
      suite("resolveDups")(
        /* test("must work") { val dups =
         * Seq( File("1_foo.md"),
         * File("1_bar.md") ) for _ <-
         * resolveDups(dups) output <-
         * TestConsole.output yield
         * assert(output.last.trim)( equalTo("2)
         * 1_bar.md") ) }, */
        test("Renaming") {
          for newName <-
              ZIO.attempt {
                val original =
                  File(
                    "/home/bfrasure/Repositories/EffectOrientedProgramming/Chapters/08_HelloTest.md"
                  )

                val stripped =
                  original
                    .getName
                    .dropWhile(_ != '_')
                    .drop(1)

                original.renameTo(
                  File(
                    original.getParent + "/" +
                      stripped
                  )
                )
              }
          yield zio.test.assert(1)(equalTo(1))
        }
      )
    )
end BookerSpec
