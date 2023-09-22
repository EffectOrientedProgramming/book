package dependency_injection

class Simple extends ZIOAppDefault:
  val effect1 =
    defer:
      val s = ZIO.service[String].run
      ZIO.debug(s).run

  val effect2 =
    defer:
      val s = ZIO.service[String].run
      ZIO.debug(s.toUpperCase).run

  val effects1and2 =
    defer:
      effect1.run
      effect2.run

  val effect3 =
    val e = defer:
      val i = ZIO.service[Int].run
      ZIO.debug(i).run
    e

  val effect1and2and3 =
    defer:
      effects1and2.run
      effect3.run

  override def run =
    effect1and2and3.provide(
      ZLayer.fail("asdf").catchAll(_ => ZLayer.succeed("zxcv")),
      ZLayer.succeed(1),
    )