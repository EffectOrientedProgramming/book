package dependency_injection

case class Flour()
case class Water()
case class Yeast()
case class Heat()

case class Bread(flour: Flour, water: Water, yeast: Yeast, heat: Heat)

val homeMadeBread: ZLayer[Flour & Water & Yeast & Heat, Nothing, Bread] =
  ZLayer.fromZIO:
    defer:
      Bread(
        ZIO.service[Flour].run,
        ZIO.service[Water].run,
        ZIO.service[Yeast].run,
        ZIO.service[Heat].run,
      )

object MakeHomeMadeBread extends ZIOAppDefault:
  override def run =
    ZIO.service[Bread].provide(
      ZLayer.succeed(Flour()),
      ZLayer.succeed(Water()),
      ZLayer.succeed(Yeast()),
      ZLayer.succeed(Heat()),
      homeMadeBread
    ).debug

case class BasicSandwich(bread: Bread)

val basicSandwich: ZLayer[Bread, Nothing, BasicSandwich] =
  ZLayer.fromZIO:
    defer:
      BasicSandwich(ZIO.service[Bread].run)

object MakeHomeMadeBasicSandwich extends ZIOAppDefault:
  override def run =
    ZIO.service[BasicSandwich].provide(
      ZLayer.succeed(Flour()),
      ZLayer.succeed(Water()),
      ZLayer.succeed(Yeast()),
      ZLayer.succeed(Heat()),
      homeMadeBread,
      basicSandwich,
    )

// hidden
val storeBread =
  Bread(Flour(), Water(), Yeast(), Heat())

val buyBread =
  ZLayer.succeed(storeBread)

object MakeBasicSandwichFromStoreBread extends ZIOAppDefault:
  override def run =
    ZIO.service[BasicSandwich].provide(
      buyBread,
      basicSandwich,
    )

