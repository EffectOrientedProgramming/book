package performance

import zio_helpers.repeatNPar

object Hedging extends ZIOAppDefault:

  def run =
    defer:
      val fastResponses =
        Ref.make(0).run
      val slowResponses =
        Ref.make(0).run

      ZIO
        .repeatNPar(50_000):
          _ =>
            defer:
              Response
                .random
                // .hedge(25.millis)
                .run match
                case Response.Fast =>
                  fastResponses.update(_ + 1).run
                case Response.BreachOfContract =>
                  slowResponses.update(_ + 1).run
        .run

      fastResponses.get
        .debug("Fast responses")
        .run
      slowResponses
        .get
        .debug("Slow responses")
        .run

end Hedging

extension [R, E, A](z: ZIO[R, E, A])
  def hedge(
             wait: zio.Duration,
           ): ZIO[R, E, A] =
    z.race:
      z
        .delay:
          wait

enum Response:
  case Fast, BreachOfContract

object Response:
  def random: ZIO[Any, Nothing, Response] =
    defer:
      val random = Random.nextIntBounded(1_000).run
      random match
        case 0 =>
          ZIO.sleep:
            3.seconds
          .run
          ZIO.succeed:
            Response.BreachOfContract
          .run
        case _ =>
          Response.Fast
