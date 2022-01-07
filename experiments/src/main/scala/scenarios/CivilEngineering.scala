package scenarios

import zio.ZIOAppArgs
import zio.{ZIOAppDefault, ZIO}

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
    UnfulfilledPromise |
      OutOfMoney |
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
    War |
      UnfulfilledPromise |
      OutOfMoney |
      PrivatePropertyRefusal,
    T
  ] =
    for
      availableCompanies <-
        Companies.operatingIn[T](state)
      legalRestrictions <-
        World.legalRestrictionsFor(state)
      politicians <- World.politiciansOf(state)
      lowestBid =
        availableCompanies
          .lowestBid(projectSpecifications)
      completedProject <- build(lowestBid)
    yield completedProject
end CivilEngineering

enum State:
  case TX, CO, CA

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
    ): ZIO[AvailableCompanies[
      Concrete
    ] & AvailableCompanies[Steel] & AvailableCompanies[UnderWaterDrilling], InsufficientResources, ProjectBid]

  trait NoValidBids

  def chooseConstructionFirm(
      firms: Set[ConstructionFirm]
  ): ZIO[Any, NoValidBids, ConstructionFirm] =
    ???
end buildABridge
