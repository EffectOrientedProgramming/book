package performance

import zio_helpers.repeatNPar

object Hedging extends ZIOAppDefault:


  extension[R, E, A] (z: ZIO[R, E, A])

    def hedge(wait: zio.Duration, depth: Int = 1): ZIO[R, E, A] =
      depth match
        case 0 => z
        case other =>
          z
            .delay:
              wait
            .race:
              hedge(wait, depth - 1)

  def hedgedRequestNarrow =
        handleRequest
          .race(handleRequest.delay(25.millis))
            .race(handleRequest.delay(25.millis))
              .race(handleRequest.delay(25.millis))

  def hedgedRequestGeneral =
    handleRequest
      .hedge(25.millis, 3)

  def run =
    defer:
      val timeBuckets =
        Ref
          .make[Map[Percentile, RequestStats]]:
            Map()
          .run

      ZIO.repeatNPar(50_000):
        demoRequest:
          timeBuckets
      .run

      pprint.pprintln(timeBuckets.get.run, width = 47)

  def demoRequest(
      timeBuckets: Ref[
        Map[Percentile, RequestStats]
      ]
  ) =
    hedgedRequestGeneral.tap(requestTime =>
      timeBuckets.update(results =>
        results.updatedWith(
          Percentile
            .fromDuration(requestTime.duration)
        ):
          case Some(value) =>
            Some(
              value.copy(
                count = value.count + 1,
                totalTime =
                  value
                    .totalTime
                    .plus(requestTime.duration)
              )
            )
          case None =>
            Some(
              RequestStats(
                count = 1,
                totalTime = requestTime.duration
              )
            )
      )
    )

end Hedging

val handleRequest =
  defer {
    Percentile.random.run match
      case Percentile._50 =>
        ResponseTimeCutoffs.Fast
      case Percentile._95 =>
        ResponseTimeCutoffs.Acceptable
      case Percentile._999 =>
        ResponseTimeCutoffs.BreachOfContract
  }.tap(dimension =>
    ZIO.sleep(dimension.duration)
  )

// TODO Fix name
enum ResponseTimeCutoffs(val duration: Duration):
  case Fast
      extends ResponseTimeCutoffs(21.millis)
  case Acceptable
      extends ResponseTimeCutoffs(70.millis)
  case BreachOfContract
      extends ResponseTimeCutoffs(1800.millis)

enum Percentile:
  case _50,
    _95,
    _999

object Percentile:
  def random =
    defer:
      val int = Random.nextIntBounded(1_000).run
      if (int < 950)
        Percentile._50
      else if (int < 999)
        Percentile._95
      else
        Percentile._999

  def fromDuration(d: Duration) =
    d match
      case ResponseTimeCutoffs.Fast.duration =>
        Percentile._50
      case ResponseTimeCutoffs.Acceptable.duration =>
        Percentile._95
      case ResponseTimeCutoffs.BreachOfContract.duration =>
        Percentile._999
      case _ => ???
end Percentile

case class RequestStats(
    count: Int,
    totalTime: Duration,
)