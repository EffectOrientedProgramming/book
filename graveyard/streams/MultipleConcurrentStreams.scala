package streams

import zio.stream.*

import java.io.File

object MultipleConcurrentStreams
    extends ZIOAppDefault:
  val userActions =
    ZStream(
      "login",
      "post:I feel happy",
      "post: I want to buy something!",
      "updateAccount",
      "logout",
      "post:I want to buy something expensive"
    ).mapZIO(action =>
      ZIO.succeed(action).delay(1.seconds)
    )
//      .throttleShape(1, 1.seconds, 2)(_.length)

  // Note: I tried to bake this into the mapZIO
  // call above, but that resulted in additional
  // printing
  // for every consumer of the stream.
  // Surprising, but I'm sure there's good
  // reasoning behind it.
  val userActionAnnouncements =
    userActions.mapZIO(action =>
      ZIO.debug("Incoming event: " + action)
    )

  val actionBytes: ZStream[Any, Nothing, Byte] =
    userActions.flatMap(action =>
      ZStream
        .fromIterable((action + "\n").getBytes)
    )
  val filePipeline
      : ZPipeline[Any, Throwable, Byte, Long] =
    ZPipeline.fromSink(
      ZSink.fromFile(new File("target/output"))
    )
  val writeActionsToFile =
    actionBytes >>> filePipeline

  val marketingData =
    userActions
      .filter(action => action.contains("buy"))

  val marketingActions =
    marketingData.mapZIO(marketingDataPoint =>
      ZIO.debug(
        "  $$ info: " + marketingDataPoint
      )
    )

  val accountAuthentication =
    userActions.filter(action =>
      action == "login" || action == "logout"
    )

  val auditingReport =
    accountAuthentication.mapZIO(event =>
      ZIO.debug("  Security info: " + event)
    )

  def run =
    ZStream
      .mergeAllUnbounded()(
        userActionAnnouncements,
        marketingActions,
        auditingReport,
        writeActionsToFile
      )
      .runDrain
end MultipleConcurrentStreams
