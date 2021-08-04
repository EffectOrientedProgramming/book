package monads
// Scratch/experimental example.

object Jackbot: // Keep from polluting the 'monads' namespace
  type Result[F, S]  = Either[F, S]
  type Fail[F, S]    = Left[F, S]
  type Succeed[F, S] = Right[F, S]
  object Fail:
    def apply[F](value: F) = Left.apply(value)
    def unapply[F, Any](fail: Fail[F, Any]) =
      Left.unapply(fail)
  object Succeed:
    def apply[S](value: S) = Right.apply(value)
    def unapply[Any, S](
        succeed: Succeed[Any, S]
    ) = Right.unapply(succeed)
end Jackbot

@main
def jackbotWroteThis =
  val ok: Jackbot.Result[Int, String] =
    Jackbot.Succeed("Hello")
  val notOk: Jackbot.Result[Int, String] =
    Jackbot.Fail(4)
  val result1 =
    ok match
      case Jackbot.Succeed(value: String) =>
        value
      case Jackbot.Fail(code: Int) =>
        s"Failed with code ${code}"
      case _ =>
        s"Exhaustive fix"
  println(result1)
  val result2 =
    notOk match
      case Jackbot.Succeed(value: String) =>
        value
      case Jackbot.Fail(code: Int) =>
        s"Failed with code ${code}"
      case _ =>
        s"Exhaustive fix"
  println(result2)
end jackbotWroteThis

/* Output:
 * Hello Failed with code 4 */
