package performance

import zio._
import zio.direct._

object Hedging extends ZIOAppDefault:

  val erraticRequest =
    ZIO.serviceWithZIO[ErraticService](
      _.handleRequest
    )

  def run =
    defer(Use.withParallelEval) {

      val timeBuckets =
        Ref
          .make[Map[Percentile, RequestStats]](
            Map()
          )
          .run

      ZIO
        .foreachPar(Range(0, 50000))(_ =>
          demoRequest(timeBuckets)
        )
        .repeat(Schedule.recurs(150))
        .run

      pprint.pprintln(timeBuckets.get.run)
    }.provide(ZLayer.succeed(ErraticService()))

  def demoRequest(
      timeBuckets: Ref[
        Map[Percentile, RequestStats]
      ]
  ) =
//    erraticRequest
    hedgedRequest.tap(requestTime =>
      timeBuckets.update(results =>
        results.updatedWith(
          Percentile
            .fromDuration(requestTime.duration)
        ) {
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
        }
      )
    )

  def hedgedRequest =
    erraticRequest
      .race(erraticRequest.delay(25.millis))

end Hedging

case class ErraticService():
  val handleRequest =
    defer {
      Percentile.random.run match
        case Percentile._50 =>
          ResponseTimeCutoffs.Fast
        case Percentile._95 =>
          ResponseTimeCutoffs.Acceptable
        case Percentile._99 =>
          ResponseTimeCutoffs.Annoying
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
  case Annoying
      extends ResponseTimeCutoffs(140.millis)
  case BreachOfContract
      extends ResponseTimeCutoffs(1800.millis)

enum Percentile:
  case _50,
    _95,
    _99,
    _999

object Percentile:
  def random =
    defer {
      val int = Random.nextIntBounded(10_000).run
      if (int < 9_500)
        Percentile._50
      else if (int < 9_900)
        Percentile._95
      else if (int < 9_999)
        Percentile._99
      else
        Percentile._999
    }

  def fromDuration(d: Duration) =
    if (d == ResponseTimeCutoffs.Fast.duration)
      Percentile._50
    else if (
      d ==
        ResponseTimeCutoffs.Acceptable.duration
    )
      Percentile._95
    else if (
      d == ResponseTimeCutoffs.Annoying.duration
    )
      Percentile._99
    else if (
      d ==
        ResponseTimeCutoffs
          .BreachOfContract
          .duration
    )
      Percentile._999
    else
      ???
end Percentile

case class RequestStats(
    count: Int,
    totalTime: Duration,
    averageResponseTime: Duration
)

object RequestStats:
  def apply(
      count: Int,
      totalTime: Duration
  ): RequestStats =
    RequestStats(
      count,
      totalTime,
      totalTime.dividedBy(count.toLong)
    )
