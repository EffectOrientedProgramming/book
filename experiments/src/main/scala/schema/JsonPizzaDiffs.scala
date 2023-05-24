package schema

import zio.*
import zio.direct.*
import zio.schema.*
import zio.schema.syntax.*
import zio.schema.codec.JsonCodec.*
import zio.stream.ZStream

object DoNotKnow extends ZIOAppDefault:

  case class Pizza(name: String, size: Int)
  object Pizza:
    implicit val schema: Schema[Pizza] =
      DeriveSchema.gen[Pizza]

  override def run =
    val pizza1 = Pizza("Fig", 16)
    val pizza2 = Pizza("Fig", 12)

    defer {
      Console.printLine(Pizza.schema).run

      val diff = pizza1 diff pizza2
      Console.printLine(diff).run

      val pizzaJson =
        jsonEncoder(Pizza.schema)
          .encodeJson(pizza1)
      Console.printLine(pizzaJson).run

      val pizzaDecoded =
        jsonDecoder(Pizza.schema)
          .decodeJson(pizzaJson)
      Console.printLine(pizzaDecoded).run

      // not applicative error
      val badDecode =
        jsonDecoder(Pizza.schema)
          .decodeJson("{}")
      Console.printLine(badDecode).run

      val jsonStream =
        ZStream.fromIterable(pizzaJson.toString)
      jsonDecoder(Pizza.schema)
        .decodeJsonStream(jsonStream)
        .debug
        .run
    }
  end run
end DoNotKnow
