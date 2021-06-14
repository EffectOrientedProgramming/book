package Hubs
import zio.*
import zio.duration
import zio.clock
import zio.console

import java.net.http.HttpResponse.ResponseInfo
//The purpose of this example to to create a very basic hub that displays small capabilities.

object BasicHub extends zio.App{


  //This example makes a hub, and publishes a String. Then, two entities take the
  //published string and print it.
  val logic1 =
    Hub.bounded[String](2).flatMap { Hub =>
      Hub.subscribe.zip(Hub.subscribe).use {
        case (left,right) =>
          for
            _ <- Hub.publish("This is from Hub left!")
            _ <- left.take.flatMap(console.putStrLn(_))
            _ <- right.take.flatMap(console.putStrLn(_))
          yield ()
      }
    }

/*
  case class entity(name:String)
  case class question(ques:String)
  case class response(rep:String, ent:entity)
  val entities = List(entity("Bob"), entity("Smith"))
  //This example sends out a question in the form of a string. Then, two
  //entities respond with different reponses.
  val logic2 =
    for
      questHub <- Hub.bounded[question](1)
      repHub <- Hub.bounded[response](entities.size)
      _ <- questHub.subscribe.zip(repHub.subscribe).use {
        case (
          Quest,
          Resp
          ) =

  }



 */

  def run(args:List[String]) =

  logic1.exitCode




}
