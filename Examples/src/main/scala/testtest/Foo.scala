package testtest

import zio.*

enum Scenario:
  case HappyPath
  case NeverWorks

val scenarioConfig: Config[Option[Scenario]] =
  Config.Optional[Scenario](Config.fail("no default scenario"))

class StaticConfigProvider(scenario: Scenario) extends ConfigProvider:
  override def load[A](config: Config[A])(implicit trace: Trace): IO[Config.Error, A] =
    ZIO.succeed(Some(scenario).asInstanceOf[A])

val happyPath =
  Runtime.setConfigProvider(StaticConfigProvider(Scenario.HappyPath))

val neverWorks =
  Runtime.setConfigProvider(StaticConfigProvider(Scenario.NeverWorks))

val saveUser =
  for
    c <- ZIO.config(scenarioConfig)
    _ <- Console.printLine(c)
    _ <- Console.printLine("hello")
  yield ()

object MyDApp extends ZIOAppDefault:
  def run =
    saveUser


object MyApp extends ZIOAppDefault:
  override val bootstrap =
    happyPath

  def run =
    saveUser


@main
def tryit =
  trait ToRun:
    val bootstrap: ZLayer[Any, Nothing, Any] = ZLayer.empty
    def run: ZIO[Any, Exception, Unit]

    def getOrThrowFiberFailure(): Unit =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.unsafe.fromLayer(bootstrap).unsafe.run(run).getOrThrowFiberFailure()
      }

  class Example99 extends ToRun:
    override val bootstrap =
      neverWorks

    def run =
      saveUser

  Example99().getOrThrowFiberFailure()
