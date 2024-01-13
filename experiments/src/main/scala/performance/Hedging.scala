package performance

import zio_helpers.repeatNPar

object Hedging extends ZIOAppDefault:

  def run =
    defer:
      val fastResponses    = Ref.make(0).run
      val contractBreaches = Ref.make(0).run

      ZIO
        .repeatNPar(50_000): _ =>
          defer:
            Response
              .random
              .hedge(25.millis)
              .run match
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

// TODO Does Hedging belong in the Composability chapter?
//   Defining extension methods that fit smoothly into your flows is impressive and should be shown somewhere.
extension [R, E, A](z: ZIO[R, E, A])
  def hedge(wait: zio.Duration): ZIO[R, E, A] =
    z.race:
      z.delay:
        wait

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
