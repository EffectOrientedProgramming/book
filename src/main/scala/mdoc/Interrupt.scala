package mdoc


def longVanillaFunction() =
  Thread.sleep(3000)
  println("done!!!")


// TODO Incorporate into Composability/AllTheThings
object Cancel extends ZIOAppDefault {
  val run =
    ZIO.attemptBlockingCancelable(
        // This does not get interrupted
      longVanillaFunction()
      )(ZIO.debug("cleaning up"))
    .timeout(50.millis)

}

object Interrupt extends ZIOAppDefault {
  val run =
    ZIO.attemptBlockingInterrupt:
      // This gets interrupted, although it takes a big performance hit
      longVanillaFunction()
    .onInterrupt(ZIO.debug("cleaning up"))
    .timeout(50.millis)

}
