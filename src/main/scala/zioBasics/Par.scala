package zioBasics

import zio.console.putStrLn
import zio.{Runtime, ZIO}

@main def par1 =
  //val i = (1 to 5).map(println(_.toString)) // todo: wyett bug please
  //val five = ZIO.collectAllPar((1 to 5).map(putStrLn(_.toString)))
  val five = ZIO.collectAllPar(
    (1 to 5).map(i => putStrLn(i.toString))
  )

  Runtime.default.unsafeRunSync(five)
