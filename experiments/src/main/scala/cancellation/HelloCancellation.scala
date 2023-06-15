package cancellation

import zio.*
import zio.direct.*

val longRunning =
  defer:
    ZIO.debug("  Started longrunning").run
    ZIO.sleep(5.seconds).run
    ZIO.debug("  Finished longrunning").run
  .onInterrupt(ZIO.debug("Interrupted LongRunning"))

object HelloCancellation extends ZIOAppDefault:

  def run =
    longRunning.timeout(2.seconds)


object HelloCancellation2 extends ZIOAppDefault:
  val complex =
    defer:
      ZIO.debug("starting complex operation").run
      longRunning.run
      ZIO.debug("finishing complex operation").run
    .onInterrupt(ZIO.debug("Interrupted Complex"))

  def run =
    complex//.timeout(2.seconds)

object CancellationWeb extends ZIOAppDefault:

  def spawnOperations(level: Int, limit: Int, parent: String): ZIO[Any, Nothing, Unit] =
    val indent = " " * (level * 2)
    if (level < limit)
      defer:
        ZIO.debug(indent + parent + " started").run
        ZIO.sleep(level.seconds).run

        ZIO.foreachPar(
          List("L", "R")
        )(
          label =>
            spawnOperations(level + 1, limit, parent + s"-$label")
        ).run

        ZIO.debug(indent + parent + " finished").run
      .onInterrupt(ZIO.debug(indent + parent + " interrupted"))
      .unit
    else
      ZIO.unit

  def run =
    spawnOperations(0, 3, "Root")
      .timeout(3.seconds)

object FailureDuringFork extends ZIOAppDefault:
  def run =
    defer {
      val fiber1 =
        defer:
          ZIO.sleep(5.seconds).run
          ZIO.debug("Complete fiber 1").run
        .onInterrupt(ZIO.debug("Interrupted fiber 1"))
        .fork
        .run


      val fiber2 =
        defer:
          ZIO.sleep(5.seconds).run
          ZIO.debug("Complete fiber 2").run
        .onInterrupt(ZIO.debug("Interrupted fiber 2"))
        .fork
        .run


      // Once we fail here, the fibers will be interrupted.
      ZIO.fail("Youch!").run
      fiber1.join.run
      fiber2.join.run
    }

