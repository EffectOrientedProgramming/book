package zioBasics

import zio.Console.printLine
import zio.{Runtime, ZIO}

@main
def par1 =
  // val i = (1 to 5).map(println(_.toString)) //
  // todo: wyett bug please
  // val five = ZIO.collectAllPar((1 to
  // 5).map(printLine(_.toString)))
  val five =
    ZIO.collectAllPar(
      (1 to 5).map(i => printLine(i.toString))
    )

  Runtime.default.unsafeRunSync(five)
end par1
