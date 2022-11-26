package streams

import zio._
import zio.stream._

object ExecutingMultipleConcurrentStreams extends ZIOAppDefault:
  val userActions =
    ZStream("login", "post:I_feel_happy", "post:_I_want_to_buy_something", "updateAccount", "logout", "post:_I_want_to_buy_something_expensive")

  val marketingData =
    userActions.filter( action => action.contains("buy"))

  val marketingActions =
    marketingData.mapZIO(marketingDataPoint => ZIO.debug("$$ info: " + marketingDataPoint))

  val accountAuthentication =
    userActions.filter(action => action == "login" || action == "logout")

  val auditingReport =
    accountAuthentication.mapZIO(event => ZIO.debug("Security info: "  + event))

  def run =
    ZStream.mergeAllUnbounded()(marketingActions, auditingReport).runDrain
