package performance

object Hedging extends ZIOAppDefault:

  def run =
    defer:
      val fastResponses    = Ref.make(0).run
      val contractBreaches = Ref.make(0).run

      ZIO
        .foreachPar(List.fill(50_000)(())): _ => // james still hates this
          defer:
            val randomResponse =
              Response.random

            val hedged =
              randomResponse.race:
                randomResponse.delay:
                  25.millis

            // todo: extract to invisible function
            hedged.run match
              case Response.Fast =>
                fastResponses.update(_ + 1).run
              case Response.BreachOfContract =>
                contractBreaches
                  .update(_ + 1)
                  .run
        .run

      fastResponses
        .get
        .debug("Fast responses")
        .run
      contractBreaches
        .get
        .debug("Slow responses")
        .run

end Hedging

// invisible below
enum Response:
  case Fast,
    BreachOfContract

object Response:
  def random: ZIO[Any, Nothing, Response] =
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
              Response.BreachOfContract
            .run
        case _ =>
          Response.Fast
