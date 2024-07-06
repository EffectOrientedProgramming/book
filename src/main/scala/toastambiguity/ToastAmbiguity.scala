package toastambiguity

import zio.*
import zio.ZIO.*
import zio.Console.*
import zio.direct.*

trait Bread:
  def eat =
    printLine:
      "Bread: Eating"

case class BreadStoreBought() extends Bread

object BreadStoreBought:
  val layer =
    ZLayer.succeed:
      BreadStoreBought()

case class Dough():
  val letRise =
    printLine:
      "Dough: rising"

object Dough:
  val fresh =
    ZLayer.fromZIO:
      defer:
        printLine("Dough: Mixed").run
        Dough()

trait HeatSource
case class Oven() extends HeatSource

val oven =
  ZLayer.fromZIO:
    defer:
      printLine("Oven: Heated").run
      Oven()

case class BreadHomeMade(
                          oven: Oven,
                          dough: Dough
                        ) extends Bread

val homemade =
  ZLayer.fromZIO:
    defer:
      printLine("BreadHomeMade: Baked").run
      BreadHomeMade(
        ZIO.service[Oven].run,
        ZIO.service[Dough].run
      )

trait Toast:
  def bread: Bread
  def heat: HeatSource
  val eat =
    printLine("Toast: Eating")

case class ToastA(heat: HeatSource, bread: Bread) extends Toast

object ToastA:
  val make =
    ZLayer.fromZIO:
      defer:
        printLine("ToastA: Made").run
        ToastA(
          ZIO.service[HeatSource].run,
          ZIO.service[Bread].run
        )

case class Toaster() extends HeatSource

object Toaster:
  val service =
    ZLayer.fromZIO:
      defer:
        printLine("Toaster: Toasting").run
        Toaster()

val ambiguous =
  ZIO
    .service[Toast]
    .provide(
      ToastA.make,
      Dough.fresh,
      homemade,
      oven,
//      Toaster.service,  // Produces ambiguity error
    )

case class ToastB(
                   heat: Toaster, // ToastA used HeatSource
                   bread: Bread
                 ) extends Toast

object ToastB:
  val make =
    ZLayer.fromZIO:
      defer:
        printLine("ToastB: Made").run
        ToastB(
          ZIO.service[Toaster].run,
          ZIO.service[Bread].run
        )


val not_ambiguous =
  ZIO
    .service[Toast]
    .provide(
      ToastB.make,
      Dough.fresh,
      homemade,
      // The two HeatSources don't clash:
      oven,
      Toaster.service,
    )


