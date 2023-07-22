package time

import zio.Console.*

val longRunning =
  ZIO.sleep(5.seconds) *> printLine("done")

val runningNotifier =
  (
    ZIO.sleep(1.seconds) *>
      printLine("Still running")
  ).onInterrupt {
    printLine("interrupted").orDie
  }

object TimedTapTapJames extends ZIOAppDefault:

  def run =
    defer {
      val lr = longRunning.fork.run
      runningNotifier.fork.run
      lr.join.run
    }

object TimedTapTapBill extends ZIOAppDefault:
  def run =
    longRunning
      .race(runningNotifier *> ZIO.never)
