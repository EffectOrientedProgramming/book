package streams

import zio.*
import zio.stream.*

import java.io.File

object ExecutingMultipleConcurrentStreams extends ZIOAppDefault:
  val userActions =
    ZStream("login", "post:I feel happy", "post: I want to buy something", "updateAccount", "logout", "post:I want to buy something expensive")
      .mapZIO(action => ZIO.sleep(1.seconds) *> ZIO.succeed(action))
//      .throttleShape(1, 1.seconds, 2)(_.length)

  val actionBytes: ZStream[Any, Nothing, Byte] =
    userActions.flatMap( action => ZStream.fromIterable((action + "\n").getBytes))
  val filePipeline: ZPipeline[Any, Throwable, Byte, Long] = ZPipeline.fromSink(
    ZSink.fromFile(new File("target/output"))
  )
  val writeActionsToFile = actionBytes >>> filePipeline

  val marketingData =
    userActions.filter( action => action.contains("buy"))

  val marketingActions =
    marketingData.mapZIO(marketingDataPoint => ZIO.debug("$$ info: " + marketingDataPoint))

  val accountAuthentication =
    userActions.filter(action => action == "login" || action == "logout")

  val auditingReport =
    accountAuthentication.mapZIO(event => ZIO.debug("Security info: "  + event))

  def run =
    ZStream.mergeAllUnbounded()(marketingActions, auditingReport, writeActionsToFile).runDrain
