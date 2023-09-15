package dependency_injection

case class A()
case class B()
case class C()
case class D(b: B, c: C)
val d: ZLayer[B & C & Scope, Nothing, D] =
  ZLayer.fromZIO:
    defer:
      val dLocal =
        D(
          ZIO.service[B].run,
          ZIO.service[C].run,
        )
      acquireReleaseDebugOnly(dLocal).run

def acquireReleaseDebugOnly[T:Tag](instance: T) =
  val rep = instance.getClass.toString.dropWhile(_ != '.').drop(1).replace("$", "")
    ZIO.acquireRelease(
      defer:
        val duration = Random.nextIntBounded(500).run
        ZIO.sleep(duration.millis).run
        ZIO.debug(s"Getting $rep").run
        instance
    )(
      _ =>
        defer:
          val duration = Random.nextIntBounded(500).run
          ZIO.sleep(duration.millis).run
          ZIO.debug(s"Releasing $rep").run
    )


def acquireReleaseDebug[T:Tag](instance: T) =
  ZLayer.fromZIO(
    acquireReleaseDebugOnly(instance)
  )

object Wow extends ZIOAppDefault {
  def run =
    defer:
      ZIO.service[A].run
      ZIO.service[B].run
      ZIO.service[C].run
      ZIO.service[D].run
    .provide(
      Scope.default,
      acquireReleaseDebug(A()),
      acquireReleaseDebug(B()),
      acquireReleaseDebug(C()),
      d
    )

}
