package dependency_injection

case class BreadDough()

val letDoughRise: ZIO[BreadDough, Nothing, Unit] =
  ZIO.unit

// Step 1: Effects can express dependencies, effects can't be run until their
//         dependencies have been fulfilled
/*
object LetDoughRiseNoDough extends ZIOAppDefault:
  override def run =
    letDoughRise
*/

object BreadDough:
  val fresh: ZLayer[Any, Nothing, BreadDough] = ZLayer.derive[BreadDough]


// Step 2: Dependencies can be provided on the effect, so that the effect can be run
object LetDoughRise extends ZIOAppDefault:
  override def run =
    letDoughRise.provide:
      BreadDough.fresh


// note: note all of the Heat vals are used right away
//       Do we organize differently or just introduce the kinds of heats
case class Heat()
object Heat:
  val oven: ZLayer[Any, Nothing, Heat] = ZLayer.derive[Heat]
  val toaster: ZLayer[Any, Nothing, Heat] = ZLayer.derive[Heat]
  val broken: ZLayer[Any, String, Nothing] = ZLayer.fail("power out")


// Step 3: Effects can require multiple dependencies
object MakeBread extends ZIOAppDefault:
  override def run =
    Bread.makeHomemade.provide(
      BreadDough.fresh,
      Heat.oven,
    )


// todo: explore a Scala 3 provideSome that doesn't need to specify the remaining types
//
//val boringSandwich: ZIO[Jelly, Nothing, BreadWithJelly] =
//  makeBreadWithJelly.provideSome[Jelly](storeBread)


case class Bread()

// note: note all of the Bread vals are used right away
//       Do we organize differently or just introduce the kinds of bread & bread actions
object Bread:
  val makeHomemade: ZIO[Heat & BreadDough, Nothing, Bread] = ZIO.succeed(Bread())
  val storeBought: ZLayer[Any, Nothing, Bread] = ZLayer.derive[Bread]
  val homemade: ZLayer[Heat & BreadDough, Nothing, Bread] = ZLayer.fromZIO(makeHomemade)

val eatBread: ZIO[Bread, Nothing, Unit] =
  ZIO.unit

// Step 4: Dependencies can "automatically" assemble to fulfill the needs of an effect
//   prose comment: something around how like typical DI, the "graph" of dependencies gets
//     resolved "for you"
object UseBread extends ZIOAppDefault:
  def run =
    eatBread.provide(
      Bread.homemade,
      BreadDough.fresh,
      Heat.oven
    )


// Dependencies of effects can have their own dependencies
case class Toast()

val makeToast: ZIO[Heat & Bread, Nothing, Toast] =
  ZIO.succeed(Toast())


// Step 5: Different effects can require the same dependency
//   The dependencies are based on the type, so in this case both
//   makeToast and makeBread require heat, but we likely do not want to
//   use the oven for both making bread and toast
object MakeToast extends ZIOAppDefault:
  override def run =
    makeToast.provide(
      Bread.homemade, // effects can provide dependencies but they also propagate their dependencies
      BreadDough.fresh,
      Heat.oven,
    )


// Step 6: Dependencies are based on types and must be uniquely provided
// Heat can't be provided twice
// Note: show mdoc compile error
/*
object MakeToastConflictingHeat extends ZIOAppDefault:
  override def run =
    makeToast.provide(
      BreadDough.fresh,
      Bread.homemade, // effects can provide dependencies but they also propagate their dependencies
      Heat.oven,
      Heat.toaster,
    )
*/


// Step 7: Effects can have their dependencies provided,
//   enabling other effects that use them to provide their own dependencies of the same type
object MakeToastWithToasterAndOven extends ZIOAppDefault:
  override def run =
    val bread = ZLayer.fromZIO(
      Bread.makeHomemade.provide(
        BreadDough.fresh,
        Heat.oven,
      )
    )

    makeToast.provide(
      bread,
      Heat.toaster,
    )


// Step 8: Dependencies can fail
object MakeBreadWithBrokenOven extends ZIOAppDefault:
  override def run =
    Bread.makeHomemade.provide(
      BreadDough.fresh,
      Heat.broken,
    ).flip.debug // todo: maybe could be a hidden extension method like prettyPrintError


// Step 9: On dependency failure, we can fallback
object MakeBreadWithBrokenOvenFallback extends ZIOAppDefault:
  override def run =
    val bread =
      ZLayer
        .fromZIO:
          Bread.makeHomemade.provide(
            BreadDough.fresh,
            Heat.broken,
          )
        .orElse:
          Bread.storeBought

    makeToast.provide(
      bread,
      Heat.toaster,
    )


// Step 10: Maybe retry on the ZLayer (BreadDough.rancid, Heat.brokenFor10Seconds)
