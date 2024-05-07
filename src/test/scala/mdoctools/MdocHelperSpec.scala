package mdoctools

import java.io.{ByteArrayOutputStream, PrintStream}

object MdocHelperSpec extends ZIOSpecDefault:
  object MdocSession:
    object App:
      object SuperDeeplyNested:
        object NameThatShouldBreakRendering:
          class CustomException extends Exception()

  def spec =
    suite("mdoc.MdocHelperSpec"):
      test("ToRun works with an Error channel"):
        class Foo extends ToRun:
          def run = Console.printLine("asdf")

        val myOut = new ByteArrayOutputStream()
        val myPs = new PrintStream(myOut)

        scala.Console.withOut(myPs):
          Foo().runAndPrintOutput()

        assertCompletes
      +
      test("ToRun works with an Error result"):
        class Foo extends ToRun:
          def run = ZIO.fail("asdf")

        val myOut = new ByteArrayOutputStream()
        val myPs = new PrintStream(myOut)

        scala.Console.withOut(myPs):
          Foo().runAndPrintOutput()

        val out = myOut.toString
        println(out)
        // TODO Make sure this stops wrapping in a Failure(...)
        // Then we can expand the assert to make sure it doesn't include mdoc bullshit and other stack traces
        assertTrue:
            out == "Result: asdf\n"
      +
      test("ToRun works with a Nothing in Error channel"):
        class Foo extends ToRun:
          def run = ZIO.unit
        Foo().runAndPrintOutput()
        assertCompletes
      +
      test("ToRun works with needing a Scope"):
        class Foo extends ToRun:
          def run = ZIO.scope
        Foo().runAndPrintOutput()
        assertCompletes
      +
      test("ToRun debug"):
        // note that ZIO.debug calls scala.Console.println so it doesn't use OurConsole
        class Foo extends ToRun:
          def run = ZIO.succeed("asdf").debug

        val myOut = new ByteArrayOutputStream()
        val myPs = new PrintStream(myOut)

        defer:
          // override the out with one we can capture
          val result = scala.Console.withOut(myPs):
            Foo().runAndPrintOutput()
          val out = myOut.toString
          assertTrue:
            out.contains("asdf")
      +
      test("OurClock is fast"):
        defer:
          val out1 = ZIO.sleep(10.seconds).timed.withClock(mdoctools.OurClock()).run
          val out2 = ZIO.sleep(100.seconds).timed.withClock(mdoctools.OurClock()).run
          assertTrue(
            out1._1.getSeconds >= 10L && out1._1.getSeconds < 13L, // the first effect has some overhead so we give it some extra room
            out2._1.getSeconds >= 100L && out1._1.getSeconds < 101L
          )
      //@@ TestAspect.nonFlaky
      +
      test("OurClock works with timeouts"):
        defer:
          val out = ZIO.sleep(10.seconds).timeout(1.second).withClock(mdoctools.OurClock()).run
          assertTrue(
            out.isEmpty
          )
      +
      test("OurClock works with long sleeps"):
        defer:
          ZIO.sleep(24.hours).withClock(mdoctools.OurClock()).run
          assertCompletes
      @@ TestAspect.timeout(1.second)
      +
      test("OurClock can be disabled"):
        defer:
          ZIO.sleep(1.second).withClock(mdoctools.OurClock(useLive = true)).run
          assertCompletes
      @@ TestAspect.nonTermination(1.second)
      +
      test("ToRun can disable OurClock"):
        class Foo extends ToRun(useLiveClock = true):
          def run = ZIO.sleep(1.second)

        Foo().runAndPrintOutput()

        assertCompletes
      @@ TestAspect.nonTermination(1.second)
      +
      test("ToTest"):
        class FooSpec extends mdoctools.ToTest:
          def spec =
            test("hello"):
              defer:
                Console.printLine("hello, world").run
                assertCompletes

        val myOut = new ByteArrayOutputStream()
        val myPs = new PrintStream(myOut)

        defer:
          // override the out with one we can capture
          val result = scala.Console.withOut(myPs):
            FooSpec().run
          .run
          val out = myOut.toString
          assertTrue:
            result.isInstanceOf[Summary] &&
            out.contains("hello, world") &&
            out.contains("\u001B[32m+\u001B[0m hello")
//      test(
//        "Intercept and format MatchError from unhandled RuntimeException"
//      ) {
//        for output <-
//              ZIO.succeed(
//                unsafeRunPrettyPrintValue(
//                  ZIO.succeed(
//                    throw new MatchError(
//                      MdocSession.App.GpsException()
//                    )
//                  )
//                )
//              )
//        yield assertTrue(
//          output == "Defect: GpsException"
//        )
//      },
//      test(
//        "Ensure successful result lines are all below length limit"
//      )(
//        for _ <-
//              ZIO
//                .attempt(
//                  runDemo(ZIO.succeed("A" * 50))
//                )
//                .flip
//        yield assertCompletes
//      ),
//      test(
//        "Concisely renders a custom Exception"
//      ) {
//        for result <-
//              ZIO
//                .attempt(
//                  runDemo(
//                    ZIO.attempt(
//                      throw MdocSession
//                        .App
//                        .SuperDeeplyNested
//                        .NameThatShouldBreakRendering
//                        .CustomException()
//                    )
//                  )
//                )
//                .debug
//        yield assertCompletes
//      },
//      test("Handle HelloFailures situation") {
//        val badMsg =
//          """
//            |error: repl.MdocSession$MdocApp$GpsException
//            |        at repl.MdocSession.MdocApp.<local MdocApp>.getTemperatureZWithFallback(14_Hello_Failures.md:250)
//            |        at mdoc.MdocHelpers$package.unsafeRunPrettyPrint(MdocHelpers.scala:78)
//            |""".stripMargin
//        for result <-
//              ZIO
//                .attempt(
//                  runDemo(ZIO.succeed(badMsg))
//                )
//                .flip
//        yield assertCompletes
//      },
//      test("Invoke failure with stack trace") {
//        for result <-
//              ZIO
//                .attempt(runDemo(ZIO.attempt(foo())))
//                .flip // TODO Better assertions around line lengths
//                .debug
//        yield assertCompletes
//      }

