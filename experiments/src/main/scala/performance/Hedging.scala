package performance

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
        .run

      pprint.pprintln(timeBuckets.get.run, width = 47)
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
    _99

object Percentile:
  def random =
    defer:
      val int = Random.nextIntBounded(100).run
      if (int < 95)
        Percentile._50
      else if (int < 99)
        Percentile._95
      else
        Percentile._99

  def fromDuration(d: Duration) =
    d match
      case ResponseTimeCutoffs.Fast.duration =>
        Percentile._50
      case ResponseTimeCutoffs.Acceptable.duration =>
        Percentile._95
      case ResponseTimeCutoffs.BreachOfContract.duration =>
        Percentile._99
      case _ => ???
end Percentile

case class RequestStats(
    count: Int,
    totalTime: Duration,
)