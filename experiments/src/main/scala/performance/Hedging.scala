package performance

import zio._
import zio.direct._

case class ErraticService():
  val handleRequest =
    defer {
      Percentile.random.run match
        case Percentile._50 =>
          DomainDomensions.Fast
        case Percentile._95 =>
          DomainDomensions.Acceptable
        case Percentile._99 =>
          DomainDomensions.Annoying
        case Percentile._999 =>
          DomainDomensions.BreachOfContract
    }.tap(dimension =>
      ZIO.sleep(dimension.duration)
    )

// TODO Fix name
enum DomainDomensions(val duration: Duration):
  case Fast extends DomainDomensions(21.millis)
  case Acceptable
      extends DomainDomensions(70.millis)
  case Annoying
      extends DomainDomensions(140.millis)
  case BreachOfContract
      extends DomainDomensions(1800.millis)

enum Percentile:
  case _50,
    _95,
    _99,
    _999

object Percentile:
  def random =
    defer {
      val int = Random.nextIntBounded(10000).run
      if (int < 950)
        Percentile._50
      else if (int < 990)
        Percentile._95
      else if (int < 9999)
        Percentile._99
      else if (int == 9999)
        Percentile._999
      else
        ???
    }

  def fromDuration(d: Duration) =
    if (d == DomainDomensions.Fast.duration)
      Percentile._50
    else if (
      d == DomainDomensions.Acceptable.duration
    )
      Percentile._95
    else if (
      d == DomainDomensions.Annoying.duration
    )
      Percentile._99
    else if (
      d ==
        DomainDomensions
          .BreachOfContract
          .duration
    )
      Percentile._999
    else
      ???
end Percentile

object Hedging extends ZIOAppDefault:
  val request =
    ZIO.serviceWithZIO[ErraticService](
      _.handleRequest
    )

  def hedgedRequest(
      timeBuckets: Ref[
        Map[Percentile, RequestStats]
      ]
  ) =
    request
      .race(
        request.delay(10.millis)
      ) // TODO Disable to show weakness
      .tap(requestTime =>
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
                  totalTime =
                    requestTime.duration
                )
              )
          }
        )
      )

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

  def run =
    defer(Use.withParallelEval) {
      val timeBuckets =
        Ref
          .make[Map[Percentile, RequestStats]](
            Map()
          )
          .run
      for _ <- Range(0, 10000) do
        hedgedRequest(timeBuckets).run
      pprint.pprintln(timeBuckets.get.run)
    }.provide(ZLayer.succeed(ErraticService()))
end Hedging
