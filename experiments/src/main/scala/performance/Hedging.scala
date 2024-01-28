package performance

object Hedging extends ZIOAppDefault:

  def run =
    defer:
      val contractBreaches = Ref.make(0).run

      ZIO
        .foreachPar(List.fill(50_000)(())): _ => // james still hates this
          defer:
            val hedged =
              logicThatSporadicallyLocksUp.race:
                logicThatSporadicallyLocksUp.delay:
                  25.millis

            // TODO How do we make this demo more obvious?
            //   The request is returning the hypothetical runtime, but that's
            //   not clear from the code that will be visible to the reader.
            val duration = logicThatSporadicallyLocksUp.run
            if (duration > 1.second)
              contractBreaches
                .update(_ + 1)
                .run
        .run

      contractBreaches
        .get
        .debug("Contract Breaches")
        .run

end Hedging

// invisible below
val logicThatSporadicallyLocksUp =
  defer:
    val random =
      Random.nextIntBounded(1_000).run
    random match
      case 0 =>
        ZIO
          .sleep:
            3.seconds
          .run
        ZIO
          .succeed:
            2.second
          .run
      case _ =>
        10.millis
