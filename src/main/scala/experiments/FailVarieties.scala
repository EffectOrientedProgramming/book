package experiments

import zio.*

case object ErrorObject1:
  val msg = "Message inside ErrorOne"
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


