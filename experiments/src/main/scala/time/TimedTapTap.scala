package time

val longRunning =
  Console
    .printLine("done")
    .delay(5.seconds)

val runningNotifier =
  defer:
    ZIO.sleep(1.seconds).run
    Console.printLine("Still running").run

object TimedTapTapJames extends ZIOAppDefault:

  def run =
    defer:
      val lr = longRunning.fork.run
      runningNotifier.fork.run
      lr.join.run
