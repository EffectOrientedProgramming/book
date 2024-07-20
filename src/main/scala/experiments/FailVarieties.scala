package experiments

import zio.*

case object ErrorObject1:
  val msg = "Message from ErrorObject1"
case object ErrorObject2 extends Exception
case object Success:
  val msg = "Happy Happy Joy Joy"

def failure(code: Int) =
  code match
    case 1 =>
      ZIO.fail("A String error")
    case 2 =>
      ZIO.fail(ErrorObject1)
    case 3 =>
      ZIO.fail(ErrorObject2)
    case _ =>
      ZIO.succeed(Success)

object FailVarieties extends ZIOAppDefault:
  def run =
    val zioEffects =
      List(1, 2, 3, 4).map:
        code =>
          failure(code).fold(
            failure =>
              Console.printLine(
                s"Failure: $failure"
              ),
            success =>
              Console.printLine(
                s"Success: ${success.msg}"
              ),
          )
    ZIO.collectAll(zioEffects)
