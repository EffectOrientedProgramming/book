package time

import zio.*
import zio.Console.*

val longRunning =
  ZIO.sleep(5.seconds) *> printLine("done")

val runningNotifier =
  (
    ZIO.sleep(1.seconds) *>
      printLine("Still running")
  ).onInterrupt {
    printLine("finalized").orDie
  }

object TimedTapTapJames extends ZIOAppDefault:

  def run =
    for
      lr <- longRunning.fork
      _  <- runningNotifier.fork
      _  <- lr.join
    yield ()

object TimedTapTapBill extends ZIOAppDefault:
  def run =
    longRunning
      .race(runningNotifier *> ZIO.never)
