package scenarios;object SideEffectingStories {
        sealed trait Fuel
        trait Gasoline extends Fuel
        trait Electricity extends Fuel
        trait ResourceLocation

        trait Earth

        trait LoadedTruck[T]:
                def unload: T

        trait LoadedFactory[T, O]:

                def produce()
                        : ZIO[Any, MachineryFailure, O]

        trait Car[T <: Fuel]:

                def drive(
                        fuel: T
                        ): IO[MachineryFailure, Unit] =
                        fuel match {
                                case gasoline: Gasoline =>
                                        def sendGasToEngine() = ???
                                                def igniteFuelInPiston() = ???
                                                ???
                                        case electricity: Electricity =>
                                                ???
                                                        def dischargeBatteryIntoElectricMotor() =
                                                                ???
                                }
                                def turnDriveShaft() = ???
                                turnDriveShaft()

        case class MachineryFailure(msg: String)
                extends Throwable

        case class CarCrash(msg: String)
                extends Throwable

        object FossilFuels:
                trait Oil
                trait DerrickExplosion

                case class OilNotFoundException()
                        extends Throwable

                case class WellIsDryException()
                        extends Throwable

                case class NoGasException()
                        extends Throwable

                trait GasStation:

                        def pumpGas(
                                car: Car[Gasoline]
                                ): ZIO[Any, NoGasException, Car[
                                Gasoline
                                ]]

                def DriveAcrossTown(car: Car[Gasoline]) =
                        def searchFor[T](): ZIO[
                                Earth,
                                OilNotFoundException,
                                ResourceLocation
                                ] =
                                ???
                                def extractOil(
                                        resourceLocation: ResourceLocation
                                        ): ZIO[
                                        Any,
                                        DerrickExplosion | WellIsDryException,
                                        Oil
                                        ] = ???
                                def putOilInTruck(
                                        oil: Oil
                                        ): ZIO[
                                        Any,
                                        MachineryFailure,
                                        LoadedTruck[Oil]
                                        ] = ???
                                def transportOilToRefinery(
                                        loadedTruck: LoadedTruck[Oil]
                                        ): ZIO[
                                        Any,
                                        CarCrash | MachineryFailure,
                                        LoadedFactory[Oil, Gasoline]
                                        ] =
                                        ???
                                def fuelUpTankerTrunk(
                                        gasoline: Gasoline
                                        ): ZIO[
                                        Any,
                                        MachineryFailure,
                                        LoadedTruck[Gasoline]
                                        ] = ???
                                def transportGasolineToStation(
                                        loadedTruck: LoadedTruck[Gasoline]
                                        ): ZIO[
                                        Any,
                                        CarCrash | MachineryFailure,
                                        GasStation
                                        ] = ???
                                def pumpGasIntoYourCar() = ???

                                for {
                                        oilLocation <- searchFor()
                                                oil <- extractOil(oilLocation)
                                                loadedTruck <- putOilInTruck(oil)
                                                refinery <- transportOilToRefinery(
                                                        loadedTruck
                                                        )
                                                gasoline <- refinery.produce()
                                                gasTanker <- fuelUpTankerTrunk(
                                                        gasoline
                                                        )
                                                gasStation <-
                                                        transportGasolineToStation(gasTanker)
                                                fueledUpCar <- gasStation.pumpGas(car)

                                        } yield ()

        object Renewables:
                trait SolarPanels
                trait Minerals
                trait Battery
                trait HOA

                case class MineralsNotFoundException()
                        extends Throwable

                case class RenewableNotProducingException()
                        extends Throwable
                case class MineCollapse() extends Throwable
                case class HOAForbidden() extends Throwable

                trait OffGridHome:

                        def generatePower(): ZIO[
                                Earth,
                                RenewableNotProducingException,
                                Electricity
                                ]

                def driveAcrossTown()
                        : ZIO[Any, Throwable, Unit] =
                        def searchForMinerals(): ZIO[
                                Earth,
                                MineralsNotFoundException,
                                ResourceLocation
                                ] = ???
                                def mineRawMinerals(
                                        mineralLocation: ResourceLocation
                                        ): IO[MineCollapse, Minerals] = ???
                                def loadMineralsIntoTruck(
                                        minerals: Minerals
                                        ): IO[MachineryFailure, LoadedTruck[
                                        Minerals
                                        ]] = ???
                                def transportMineralsToBatteryFactory(
                                        loadedTruck: LoadedTruck[Minerals]
                                        ): IO[
                                        CarCrash | MachineryFailure,
                                        LoadedFactory[Minerals, Battery]
                                        ] = ???
                                def transportMineralsToSolarPanelFactory(
                                        loadedTruck: LoadedTruck[Minerals]
                                        ): IO[
                                        CarCrash | MachineryFailure,
                                        LoadedFactory[Minerals, SolarPanels]
                                        ] =
                                        ???
                                def installSolarPanelsOnRoof(
                                        solarPanels: SolarPanels
                                        ): ZIO[HOA, HOAForbidden, OffGridHome] =
                                        ???
                                def transmitSolarPowerToVehicleBattery(
                                        offGridHome: OffGridHome,
                                        car: Car[Electricity]
                                        ): Car[Electricity] = ???

                                for {
                                        mineralsLocal <- searchForMinerals()
                                                .retry(Schedule())
                                                minerals <- mineRawMinerals(
                                                        mineralsLocal
                                                        )
                                                loadedTruck <- loadMineralsIntoTruck(
                                                        minerals
                                                        )
                                                batteryFactory <-
                                                        transportMineralsToBatteryFactory(
                                                                loadedTruck
                                                                )
                                                                .catchAll {
                                                                case crash: CarCrash => ???
                                                                        case failure: MachineryFailure =>
                                                                                ???
                                                                }
                                                battery <- batteryFactory.produce()
                                                solarPanelFactory <-
                                                        transportMineralsToSolarPanelFactory(
                                                                loadedTruck
                                                                ) // Ack! This is basically a use-after-free
                                                solarPanels <- solarPanelFactory
                                                        .produce()
                                                offGridHome <-
                                                        installSolarPanelsOnRoof(solarPanels)
                                        } yield ()
                                ???

        }
