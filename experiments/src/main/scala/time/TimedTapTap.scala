package time

import zio.Console.*

val longRunning =
  defer {
    ZIO.sleep(5.seconds).run
    printLine("done").run
  }

val runningNotifier =
  defer {
    ZIO.sleep(1.seconds).run
    printLine("Still running").run
  }.onInterrupt {
    printLine("interrupted").orDie
  }

object TimedTapTapJames extends ZIOAppDefault:

  def run =
    defer {
      val lr = longRunning.fork.run
      runningNotifier.fork.run
      lr.join.run
    }

import scala.annotation.nowarn

object TimedTapTapBill extends ZIOAppDefault:

  @nowarn
  def run =
    longRunning
      //      .race{ runningNotifier *> ZIO.never}

      .race {
        defer {
          runningNotifier.run
          ZIO.never.run
          // TODO - Get zio-direct fixed so that it doesn't need this
          ()
        }
      }
