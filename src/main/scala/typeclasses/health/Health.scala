package typeclasses.health

case class UUID(value: String)

object UUID:

  def randomUUID() = UUID(
    System.nanoTime().toString
  )

object Canonical:
  case class PatientId(raw: String)
  case class HealthCareProviderId(raw: String)

  case class CategorizingInfo(
      patientId: PatientId,
      providerId: HealthCareProviderId
  )

  trait Categorizer[T]:

    extension (t: T)
      def categorizingInfo(): CategorizingInfo

  case class CategorizedData[A: Categorizer](
      data: A
  ):

    def hasConsentedToShare()
        : Boolean = // Real impl would consult a Map, DB, etc
      if (
        data
          .categorizingInfo()
          .providerId
          .raw == "epic.provider_1" && data
          .categorizingInfo()
          .patientId
          .raw ==
          "epic.patient_1"
      )
        true
      else
        false

  def filterAll(
      data: List[CategorizedData[_]]
  ): List[_] =
    data.filter(_.hasConsentedToShare())

object Medicare: // Flat, stringly-typed data

  case class Appointment(
      patientId: String,
      providerId: String,
      details: String
  )

object Epic: // Structured, stringly-typed data
  case class Patient(id: String)
  case class HealthCareProvider(id: String)

  case class Visit(
      patient: Patient,
      healthCareProvider: HealthCareProvider
  )

object UpStartHealth:
  case class MedPatient(uuid: UUID)
  case class PublicHealthProvider(uuid: UUID)

  case class TestResults(
      medPatient: MedPatient,
      publicHealthProvider: PublicHealthProvider
  )

object InterOp:
  import Canonical.{
    Categorizer,
    CategorizingInfo,
    HealthCareProviderId,
    PatientId
  }
  import UpStartHealth.TestResults

  given Categorizer[TestResults] with

    extension (a: TestResults)

      def categorizingInfo()
          : CategorizingInfo =
        CategorizingInfo(
          PatientId(
            a.medPatient.uuid.toString
          ),
          providerId = HealthCareProviderId(
            a.publicHealthProvider.uuid.toString
          )
        )

  import Epic.Visit

  given Categorizer[Visit] with

    extension (a: Visit)

      def categorizingInfo()
          : CategorizingInfo =
        CategorizingInfo(
          PatientId("epic." + a.patient.id),
          HealthCareProviderId(
            "epic." + a.healthCareProvider.id
          )
        )

@main def healthStuff() =
  import Canonical._
  import InterOp.given

  val firstPieceOfData =
    CategorizedData(
      Epic.Visit(
        patient = Epic.Patient("patient_1"),
        healthCareProvider =
          Epic.HealthCareProvider("provider_1")
      )
    )

  val secondPieceOfData =
    CategorizedData(
      data = UpStartHealth.TestResults(
        medPatient = UpStartHealth.MedPatient(
          UUID.randomUUID()
        ),
        publicHealthProvider = UpStartHealth
          .PublicHealthProvider(uuid =
            UUID.randomUUID()
          )
      )
    )

  println(
    filterAll(
      List(
        firstPieceOfData,
        secondPieceOfData
      )
    ) // == List(firstPieceOfData)
  )
