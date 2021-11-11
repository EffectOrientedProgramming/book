package scenarios

import zio.ZIOAppArgs
import zio.{ZIOAppDefault, ZIO, Has}

object CivilEngineering extends ZIOAppDefault {
  trait Company[T]
  trait ProjectSpecifications[T]
  trait LegalRestriction
  trait War
  
  val run =
    ???
  
  val installPowerLine =
    ???
    
    
  trait World
  object World:
    def legalRestrictionsFor(state: State): ZIO[Has[World], War, Set[LegalRestriction]] = ???
    def politicansOf(state: State): ZIO[Has[World], War, Set[LegalRestriction]] = ???
  
    
  def stateBid[T](state: State, projectSpecifications: ProjectSpecifications[T]) =
    for
      legalRestrictions <- World.legalRestrictionsFor(state)
      politicians <- World.politicansOf(state)
    yield  ???
  
}

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
    ): ZIO[Has[
      AvailableCompanies[Concrete]
    ] with Has[
      AvailableCompanies[Steel]
    ] with Has[
      AvailableCompanies[UnderWaterDrilling]
    ], InsufficientResources, ProjectBid]

  trait NoValidBids

  def chooseConstructionFirm(
      firms: Set[ConstructionFirm]
  ): ZIO[Any, NoValidBids, ConstructionFirm] =
    ???
end buildABridge
