package grammar

class GrammartBot {}

import zio.Console.printLine
import zio.Console.readLine
import zio.ZIO
import zio.System.env

object GrammarInteractions
    extends zio.ZIOAppDefault:
  final def run = myAppLogic.exitCode

  val myAppLogic =
    for
      apiKey <- env("GRAMMARBOT_API_KEY")
      _      <- printLine(s"API KEY $apiKey")
      _ <-
        ZIO.succeed {
          /* import sttp.client3._ import
           * sttp.client3.circe._ import
           * io.circe.generic.auto._ import
           * io.circe.syntax._ import
           * zio.IsSubtypeOfOutput.impl
           *
           * case class GrammarBotSubmission(
           * text: String )
           *
           * val backend =
           * HttpURLConnectionBackend()
           *
           * val response =
           * basicRequest .body( Map( "text" ->
           * "I can has something." ) )
           * .header("x-rapidapi-key", apiKey)
           * .header( "x-rapidapi-host",
           * "grammarbot.p.rapidapi.com" ) .post(
           * uri"https://grammarbot.p.rapidapi.com/check"
           * ) .send(backend)
           *
           * println(response.body) */
        }
    yield ()
end GrammarInteractions

// TODO Revive pieces of code from this commit:
// 150090f43f2a788ed50232f4a1cf07db31bb2527
// Now, instead of the fenced content, we _only_
// want prose
