package fibers

import zio.{Console, Unsafe}
import zio.Duration.fromMillis
import zio.Schedule.recurs
import zio.Runtime.default.unsafe

@main
def forDemo() =
  val logic =
    for
      a <-
        Console
          .printLine("Application HeartBeat")
          .delay(fromMillis(2000))
          .repeat(recurs(2))
          .fork
      b <-
        Console
          .printLine("Processing Item")
          .delay(fromMillis(700))
          .repeat(recurs(6))
          .fork
      res <- b.join
    yield println("done")

//  unsafeRun(logic)
  Unsafe.unsafeCompat { implicit u =>
    unsafe.run(logic).getOrThrowFiberFailure()
  }
end forDemo
