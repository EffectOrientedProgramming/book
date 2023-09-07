package cancellation

val longRunning =
  createProcess(
    "LongRunning",
    ZIO.sleep(5.seconds)
  )

object HelloCancellation extends ZIOAppDefault:

  def run = longRunning.timeout(2.seconds)

def createProcess(
                   label: String,
                   innerProcess: ZIO[Any, Nothing, Unit]
                 ) =
  defer:
    ZIO.debug(s"Started $label").run
    innerProcess.run
    ZIO.debug(s"Finished $label").run
  .onInterrupt(ZIO.debug(s"Interrupted $label"))


object HelloCancellation2 extends ZIOAppDefault:
  val complex =
    createProcess("Complex", longRunning)

  def run = complex.timeout(2.seconds)

object CancellationWeb extends ZIOAppDefault:
  def spawnLevel(
      level: Int,
      limit: Int,
      parent: String
  ): ZIO[Any, Nothing, Unit] =
    ZIO
      .foreachPar(List("L", "R"))(label =>
        createProcess(
          " " *
            (level + 1 * 2) + parent +
            s"-$label",
          ZIO
            .when(level < limit)(
              spawnLevel(
                level + 1,
                limit,
                " " *
                  (level + 1 * 2) + parent +
                  s"-$label"
              )
            )
            .unit
        )
      )
      .delay(level.seconds)
      .unit

  def run =
    spawnLevel(0, 3, "Root").timeout(3.seconds)
end CancellationWeb

object FailureDuringFork extends ZIOAppDefault:
  def run =
    defer {
      val fiber1 =
        createProcess(
          "Fiber 1",
          ZIO.sleep(5.seconds)
        ).fork
         .run


      val fiber2 =
        createProcess(
          "Fiber 2",
          ZIO.sleep(5.seconds)
        ).fork
         .run

      // Once we fail here, the fibers will be
      // interrupted.
      ZIO.fail("Youch!").run
      fiber1.join.run
      fiber2.join.run
    }
end FailureDuringFork

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.text.similarity.LevenshteinDistance

val input  = RandomStringUtils.random(70_000)
val target = RandomStringUtils.random(70_000)
val leven =
  LevenshteinDistance.getDefaultInstance

object PlainLeven extends App:
  leven(input, target)

object CancellingATightLoop
    extends ZIOAppDefault:
  val scenario =
    ZIO
      .attempt(leven(input, target))

  def run =
    // For timeouts, you need fibers and
    // cancellation
    scenario
      // TODO This is running for 16 seconds
      // nomatter what.
      .timed.debug("Time:")
      .timeout(2.seconds)
