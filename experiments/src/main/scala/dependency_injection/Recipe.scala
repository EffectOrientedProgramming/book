package dependency_injection

case class Flour()
object Flour:
  val live = ZLayer.derive[Flour]

case class Water()
object Water:
  val live = ZLayer.derive[Water]

case class Yeast()
object Yeast:
  val live = ZLayer.derive[Yeast]

case class Heat()
object Heat:
  val oven = ZLayer.derive[Heat]
  val toaster = ZLayer.derive[Heat]

case class Bread()
object Bread:
  val live = ZLayer.derive[Bread]

// hidden
val preheatOven: ZIO[Heat, Nothing, Unit] =
  ???

/*
// Does not compile but shows that dependencies can be expressed
object PreheatOvenNoHeat extends ZIOAppDefault:
  override def run =
    preheatOven
*/

// Dependencies are required to run an effect
// When you get to `run` there can't be any dependencies left
object PreheatOven extends ZIOAppDefault:
  override def run =
    preheatOven.provide(
      Heat.oven
    )


case class BreadDough(flour: Flour, water: Water, yeast: Yeast)
object BreadDough:
  val live = ZLayer.succeed(
    BreadDough(Flour(), Water(), Yeast())
  )

val makeBread: ZIO[Heat & BreadDough, Nothing, Bread] =
  ???


// Effects can require multiple dependencies
// now we can cook something
object MakeBread extends ZIOAppDefault:
  override def run =
    makeBread.provide(
      BreadDough.live,
      Heat.oven,
    )


case class Jelly()
case class BreadWithJelly()

val makeBreadWithJelly: ZIO[Bread & Jelly, Nothing, BreadWithJelly] =
  ???


// Provide a dependency so that it doesn't propagate
// Dependencies can be provided at any level removing their requirement propagation
object BreadWithJelly extends ZIOAppDefault:
  override def run =
    val bread = ZLayer.fromZIO(
      makeBread.provide(
        BreadDough.live,
        Heat.oven,
      )
    )

    makeBreadWithJelly.provide(
      bread,
      ZLayer.succeed(Jelly()),
    )


// Dependencies of effects can have their own dependencies
case class Toast()

val makeToast: ZIO[Heat & Bread, Nothing, Toast] =
  ???


// todo: is using Heat.live the right semantics for the different kinds of heat
//   or can we do it the wrong way (making toast in the oven) then in the next step
//   instead make the toast in a toaster
object MakeToast extends ZIOAppDefault:
  override def run =
    makeToast.provide(
      BreadDough.live,
      ZLayer.fromZIO(makeBread), // effects can provide dependencies but they also propagate their dependencies
      Heat.oven,
    )


// Dependencies can then reintroduce dependencies that were previously provided
object MakeToastWithToasterAndOven extends ZIOAppDefault:
  override def run =
    val bread = ZLayer.fromZIO(
      makeBread.provide(
        BreadDough.live,
        Heat.oven,
      )
    )

    makeToast.provide(
      bread,
      Heat.toaster,
    )


// todo: maybe toasted bread with jelly

// todo: a test with different dependencies