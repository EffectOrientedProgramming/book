package test_aspects

import zio.*
import zio.test.*

trait DemoBaseSpec extends ZIOSpecDefault:
  val trackStats =
    aroundAllWith(ZIO.debug("Starting"))(
      (_: Unit) => ZIO.debug("Finishing")
    )
  override def aspects: Chunk[
    TestAspectAtLeastR[TestEnvironment]
  ] =
    if (TestPlatform.isJVM)
      Chunk(
        TestAspect.timeout(10.seconds),
        TestAspect.timed,
        trackStats
      )
    else
      Chunk(
        TestAspect.timeout(10.seconds),
        TestAspect.sequential,
        TestAspect.timed,
        trackStats
      )

  def aroundWith[R0, E0, A0](
      before: ZIO[R0, E0, A0]
  )(
      after: A0 => ZIO[R0, Nothing, Any]
  ): TestAspect[Nothing, R0, E0, Any] =
    new TestAspect.PerTest[Nothing, R0, E0, Any]:
      def perTest[R <: R0, E >: E0](
          test: ZIO[R, TestFailure[
            E
          ], TestSuccess]
      )(implicit
          trace: Trace
      ): ZIO[R, TestFailure[E], TestSuccess] =
        ZIO.acquireReleaseWith(
          before.catchAllCause(c =>
            ZIO.fail(TestFailure.Runtime(c))
          )
        )(after)(_ => test)

  def walk[R, E](
      spec: Spec[R, E],
      labels: Chunk[String] = Chunk.empty
  ): Unit =
    println("Walking")
    spec.caseValue match
      case Spec.ExecCase(exec, spec) =>
        ()
      case Spec.LabeledCase(label, spec) =>
        println("Walk Label: " + label)
        walk(spec, labels.appended(label))
      case Spec.ScopedCase(scoped) =>
        ()
      case Spec.MultipleCase(specs) =>
        println("Multi case")
        specs.foreach(s => walk(s, labels))
      case Spec.TestCase(test, annotations) =>
        println(
          "test labels: " +
            labels.mkString(" - ")
        )
        ()
  end walk

  def aroundAllWith[R0, E0, A0](
      before: ZIO[R0, E0, A0]
  )(
      after: A0 => ZIO[R0, Nothing, Any]
  ): TestAspect[Nothing, R0, E0, Any] =
    new TestAspect[Nothing, R0, E0, Any]:
      def some[R <: R0, E >: E0](
          spec: Spec[R, E]
      )(implicit trace: Trace): Spec[R, E] =
        walk(spec)
        Spec.scoped[R](
          ZIO
            .acquireRelease(before)(after)
            .mapError(TestFailure.fail)
            .as(spec)
        )
end DemoBaseSpec

object Demo1Spec extends DemoBaseSpec:
  def spec =
    suite("Demo1Spec")(
      test("test1") {
        assertCompletes
      },
      test("test2") {
        assertCompletes
      }
    )
