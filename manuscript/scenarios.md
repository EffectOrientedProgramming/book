## scenarios

 

### experiments/src/main/scala/scenarios/CivilEngineering.scala
```scala
package scenarios

object CivilEngineering extends ZIOAppDefault:
  trait Company[T]:
    def produceBid(
        projectSpecifications: ProjectSpecifications[
          T
        ]
    ): ProjectBid[T]
  object Companies:
    def operatingIn[T](
        state: State
    ): ZIO[World, Nothing, AvailableCompanies[
      T
    ]] = ???

  trait ProjectSpecifications[T]
  trait LegalRestriction
  case class War(reason: String)
  trait UnfulfilledPromise
  trait ProjectBid[T]

  val run = ???

  val installPowerLine = ???

  case class AvailableCompanies[T](
      companies: Set[Company[T]]
  ):
    def lowestBid(
        projectSpecifications: ProjectSpecifications[
          T
        ]
    ): ProjectBid[T] = ???

  trait World
  object World:
    def legalRestrictionsFor(
        state: State
    ): ZIO[World, War, Set[LegalRestriction]] =
      ???
    def politiciansOf(
        state: State
    ): ZIO[World, War, Set[LegalRestriction]] =
      ???

  trait OutOfMoney

  trait PrivatePropertyRefusal
  def build[T](projectBid: ProjectBid[T]): ZIO[
    Any,
    UnfulfilledPromise | OutOfMoney |
      PrivatePropertyRefusal,
    T
  ] = ???

  def stateBid[T](
      state: State,
      projectSpecifications: ProjectSpecifications[
        T
      ]
  ): ZIO[
    World,
    War | UnfulfilledPromise | OutOfMoney |
      PrivatePropertyRefusal,
    T
  ] =
    defer {
      val availableCompanies =
        Companies.operatingIn[T](state).run
      val legalRestrictions =
        World.legalRestrictionsFor(state).run
      val politicians =
        World.politiciansOf(state).run
      val lowestBid =
        availableCompanies
          .lowestBid(projectSpecifications)
      build(lowestBid).run
    }
end CivilEngineering

enum State:
  case TX,
    CO,
    CA

def buildABridge() =
  trait Company[T]
  trait Surveyor
  trait CivilEngineer
  trait ProjectSpecifications
  trait Specs[Service]
  trait LegalRestriction

  trait ProjectBid
  trait InsufficientResources

  def createProjectSpecifications(): ZIO[
    Any,
    LegalRestriction,
    ProjectSpecifications
  ] = ???

  case class AvailableCompanies[T](
      companies: Set[Company[T]]
  )

  trait Concrete
  trait Steel
  trait UnderWaterDrilling

  trait ConstructionFirm:
    def produceBid(
        projectSpecifications: ProjectSpecifications
    ): ZIO[
      AvailableCompanies[Concrete] &
        AvailableCompanies[Steel] &
        AvailableCompanies[UnderWaterDrilling],
      InsufficientResources,
      ProjectBid
    ]

  trait NoValidBids

  def chooseConstructionFirm(
      firms: Set[ConstructionFirm]
  ): ZIO[Any, NoValidBids, ConstructionFirm] =
    ???
end buildABridge

```


### experiments/src/main/scala/scenarios/SecuritySystem.scala
```scala
package scenarios

import zio.Console.printLine

import scala.concurrent.TimeoutException
import time.scheduledValues
import izumi.reflect.Tag

case class TempSense(
    z: ZIO[
      Any,
      HardwareFailure,
      ZIO[Any, TimeoutException, Degrees]
    ]
)

case class SecuritySystemX(
    motionDetector: MotionDetector,
    thermalDetectorX: ThermalDetectorX,
    acousticDetectorX: AcousticDetectorX
)

object SecuritySystemX:
  val live =
    ZLayer.fromFunction(SecuritySystemX.apply _)

/** Situations: Security System: Should monitor
  *   - Motion
  *   - Heat/Infrared
  *   - Sound Should alert by:
  *   - Quiet, local beep
  *   - Loud Local Siren
  *   - Ping security company
  *   - Notify police
  */
object SecuritySystem:
  // TODO Why can't I use this???
  val s: zio.ZLayer[
    Any,
    Nothing,
    scenarios.TempSense
  ] =
    SensorData.live[Degrees, TempSense](
      x => TempSense(x),
      (1.seconds, Degrees(71)),
      (2.seconds, Degrees(70))
    )

  val accessMotionDetector: ZIO[
    scenarios.MotionDetector,
    scenarios.HardwareFailure,
    scenarios.Pixels
  ] = ZIO.serviceWithZIO(_.amountOfMotion())

  def securityLoop(
      amountOfHeatGenerator: ZIO[
        Any,
        scala.concurrent.TimeoutException |
          scenarios.HardwareFailure,
        scenarios.Degrees
      ],
      amountOfMotion: Pixels,
      acousticDetector: ZIO[
        Any,
        scala.concurrent.TimeoutException |
          scenarios.HardwareFailure,
        scenarios.Decibels
      ]
  ): ZIO[
    SirenX,
    scala.concurrent.TimeoutException |
      HardwareFailure,
    Unit
  ] =
    defer {
      val amountOfHeat =
        amountOfHeatGenerator.run
      val noise = acousticDetector.run
      ZIO
        .debug(
          s"Heat: $amountOfHeat  Motion: $amountOfMotion  Noise: $noise"
        )
        .run
      val securityResponse =
        determineResponse(
          amountOfMotion,
          amountOfHeat,
          noise
        )
      securityResponse match
        case Relax =>
          ZIO.debug("No need to panic").run
        case LowBeep =>
          SirenX.lowBeep.run
        case LoudSiren =>
          SirenX.loudSiren.run
    }

  def shouldAlertServices[
      T
        <: MotionDetector & ThermalDetectorX &
          SirenX & AcousticDetectorX
  ](): ZIO[
    T,
    scenarios.HardwareFailure | TimeoutException,
    String
  ] =
    defer {
      val amountOfMotion =
        MotionDetector
          .acquireMotionMeasurementSource()
          .run
      val amountOfHeatGenerator =
        ThermalDetectorX
          .acquireHeatMeasurementSource
          .run
      val acousticDetector =
        AcousticDetectorX.acquireDetector.run
      securityLoop(
        amountOfHeatGenerator,
        amountOfMotion,
        acousticDetector
      ).repeat(
          Schedule.recurs(5) &&
            Schedule.spaced(1.seconds)
        )
        .run
      "Fin"
    }

  def shouldTrigger(
      amountOfMotion: Pixels,
      amountOfHeat: Degrees
  ): Boolean =
    amountOfMotion.value > 10 &&
      amountOfHeat.value > 95

  def determineResponse(
      amountOfMotion: Pixels,
      amountOfHeat: Degrees,
      noise: Decibels
  ): SecurityResponse =
    val numberOfAlerts =
      List(
        amountOfMotion.value > 50,
        amountOfHeat.value > 95,
        noise.value > 15
      ).filter(_ == true).length

    if (numberOfAlerts == 0)
      Relax
    else if (numberOfAlerts == 1)
      LowBeep
    else
      LoudSiren
  end determineResponse

  def determineBreaches(
      amountOfMotion: Pixels,
      amountOfHeat: Degrees,
      noise: Decibels
  ): Set[SecurityBreach] =
    List(
      Option.when(amountOfMotion.value > 50)(
        SignificantMotion
      ),
      Option.when(
        amountOfHeat.value > 95 &&
          amountOfHeat.value < 200
      )(BodyHeat),
      Option
        .when(amountOfHeat.value >= 200)(Fire),
      Option.when(noise.value > 15)(LoudNoise)
    ).flatten.toSet

end SecuritySystem

trait SecurityBreach
object BodyHeat          extends SecurityBreach
object Fire              extends SecurityBreach
object LoudNoise         extends SecurityBreach
object SignificantMotion extends SecurityBreach

trait SecurityResponse
object Relax     extends SecurityResponse
object LowBeep   extends SecurityResponse
object LoudSiren extends SecurityResponse

trait HardwareFailure

case class Decibels(value: Int)
case class Degrees(value: Int)
case class Pixels(value: Int)

trait MotionDetector:
  def amountOfMotion()
      : ZIO[Any, HardwareFailure, Pixels]

object MotionDetector:

  object LiveMotionDetector
      extends MotionDetector:
    override def amountOfMotion()
        : ZIO[Any, HardwareFailure, Pixels] =
      ZIO.succeed(Pixels(30))

  def acquireMotionMeasurementSource(): ZIO[
    MotionDetector,
    HardwareFailure,
    Pixels
  ] =
    ZIO
      .service[MotionDetector]
      .flatMap(_.amountOfMotion())

  val live
      : ZLayer[Any, Nothing, MotionDetector] =
    ZLayer.succeed(LiveMotionDetector)
end MotionDetector

trait ThermalDetectorX:
  def heatMeasurementSource()
      : ZIO[Any, Nothing, ZIO[
        Any,
        TimeoutException |
          scenarios.HardwareFailure,
        Degrees
      ]]

trait ThermalDetectorY:
  def heatMeasurement(): ZIO[
    Any,
    TimeoutException | scenarios.HardwareFailure,
    Degrees
  ]

object ThermalDetectorY:

  def apply(
      value: (Duration, Degrees),
      values: (Duration, Degrees)*
  ): ZLayer[Any, Nothing, ThermalDetectorY] =
    ZLayer.fromZIO(
      defer {
        val thermalDetectorValues =
          scheduledValues(value, values*).run
        ZIO
          .succeed(
            new ThermalDetectorY:
              override def heatMeasurement()
                  : ZIO[
                    Any,
                    TimeoutException |
                      scenarios.HardwareFailure,
                    Degrees
                  ] = thermalDetectorValues
          )
          .run
      }
    )
end ThermalDetectorY

object ThermalDetectorX:

  def apply(
      value: (Duration, Degrees),
      values: (Duration, Degrees)*
  ): ZLayer[Any, Nothing, ThermalDetectorX] =
    ZLayer.succeed(
      // that same service we wrote above
      new ThermalDetectorX:
        override def heatMeasurementSource()
            : ZIO[Any, Nothing, ZIO[
              Any,
              TimeoutException |
                scenarios.HardwareFailure,
              Degrees
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  def acquireHeatMeasurementSource[
      T <: scenarios.ThermalDetectorX: Tag
  ]: ZIO[T, Nothing, ZIO[
    Any,
    scala.concurrent.TimeoutException |
      scenarios.HardwareFailure,
    scenarios.Degrees
  ]] =
    ZIO.serviceWithZIO[ThermalDetectorX](
      _.heatMeasurementSource()
    )

end ThermalDetectorX

trait AcousticDetectorX:
  def acquireDetector(): ZIO[Any, Nothing, ZIO[
    Any,
    TimeoutException | scenarios.HardwareFailure,
    Decibels
  ]]

object AcousticDetectorX:

  def apply(
      value: (Duration, Decibels),
      values: (Duration, Decibels)*
  ): ZLayer[Any, Nothing, AcousticDetectorX] =
    ZLayer.succeed(
      // that same service we wrote above
      new AcousticDetectorX:
        override def acquireDetector()
            : ZIO[Any, Nothing, ZIO[
              Any,
              TimeoutException |
                scenarios.HardwareFailure,
              Decibels
            ]] = scheduledValues(value, values*)
    )

  // This is preeeetty gnarly. How can we
  // improve?
  def acquireDetector[
      T <: scenarios.AcousticDetectorX: Tag
  ]: ZIO[T, Nothing, ZIO[
    Any,
    scala.concurrent.TimeoutException |
      scenarios.HardwareFailure,
    scenarios.Decibels
  ]] =
    ZIO.serviceWithZIO[
      scenarios.AcousticDetectorX
    ](_.acquireDetector())

end AcousticDetectorX

object Siren:
  trait ServiceX:
    def lowBeep(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ]

  val live
      : ZLayer[Any, Nothing, Siren.ServiceX] =
    ZLayer.succeed(
      // that same service we wrote above
      new ServiceX:

        def lowBeep(): ZIO[
          Any,
          scenarios.HardwareFailure,
          Unit
        ] = ZIO.debug("beeeeeeeeeep")
    )
end Siren

trait SirenX:
  def lowBeep()
      : ZIO[Any, scenarios.HardwareFailure, Unit]

  def loudSiren()
      : ZIO[Any, scenarios.HardwareFailure, Unit]

object SirenX:
  object SirenXLive extends SirenX:
    def lowBeep(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ] = ZIO.debug("beeeeeeeeeep")

    def loudSiren(): ZIO[
      Any,
      scenarios.HardwareFailure,
      Unit
    ] = ZIO.debug("WOOOO EEEE WOOOOO EEEE")

  val live: ZLayer[Any, Nothing, SirenX] =
    ZLayer.succeed(SirenXLive)

  val lowBeep: ZIO[
    SirenX,
    scenarios.HardwareFailure,
    Unit
  ] = ZIO.serviceWith(_.lowBeep())

  val loudSiren: ZIO[
    SirenX,
    scenarios.HardwareFailure,
    Unit
  ] = ZIO.serviceWith(_.loudSiren())

end SirenX

class SensorD[T](
    z: ZIO[
      Any,
      HardwareFailure,
      ZIO[Any, TimeoutException, T]
    ]
)

// TODO Figure out how to use this
object SensorData:
  def live[T, Y: zio.Tag](
      c: ZIO[
        Any,
        HardwareFailure,
        ZIO[Any, TimeoutException, T]
      ] => Y,
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, Y] =
    ZLayer.succeed(
      // that same service we wrote above
      c(scheduledValues[T](value, values*))
    )

  def liveS[T: zio.Tag](
      value: (Duration, T),
      values: (Duration, T)*
  ): ZLayer[Any, Nothing, SensorD[T]] =
    ZLayer.succeed(
      // that same service we wrote above
      SensorD(scheduledValues[T](value, values*))
    )
end SensorData

```


