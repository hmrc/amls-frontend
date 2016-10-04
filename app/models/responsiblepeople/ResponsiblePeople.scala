package models.responsiblepeople

import play.Logger
import typeclasses.MongoKey
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class ResponsiblePeople(personName: Option[PersonName] = None,
                             personResidenceType: Option[PersonResidenceType] = None,
                             contactDetails: Option[ContactDetails] = None,
                             addressHistory: Option[ResponsiblePersonAddressHistory] = None,
                             positions: Option[Positions] = None,
                             saRegistered: Option[SaRegistered] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             experienceTraining: Option[ExperienceTraining] = None,
                             training: Option[Training] = None,
                             hasAlreadyPassedFitAndProper: Option[Boolean] = None,
                             hasChanged: Boolean = false,
                             lineId: Option[Int] = None,
                             status: Option[String] = None
                          ) {

  def personName(p: PersonName): ResponsiblePeople =
    this.copy(personName = Some(p), hasChanged = hasChanged || !this.personName.contains(p))

  def personResidenceType(p: PersonResidenceType): ResponsiblePeople =
    this.copy(personResidenceType = Some(p), hasChanged = hasChanged || !this.personResidenceType.contains(p))

  def personResidenceType(p: Option[PersonResidenceType]): ResponsiblePeople =
    this.copy(personResidenceType = p, hasChanged = hasChanged || this.personResidenceType != p)

  def contactDetails(p: ContactDetails): ResponsiblePeople =
    this.copy(contactDetails = Some(p), hasChanged = hasChanged || !this.contactDetails.contains(p))

  def saRegistered(p: SaRegistered): ResponsiblePeople =
    this.copy(saRegistered = Some(p), hasChanged = hasChanged || !this.saRegistered.contains(p))

  def addressHistory(p: ResponsiblePersonAddressHistory): ResponsiblePeople =
    this.copy(addressHistory = Some(p), hasChanged = hasChanged || !this.addressHistory.contains(p))

  def positions(p: Positions): ResponsiblePeople =
    this.copy(positions = Some(p), hasChanged = hasChanged || !this.positions.contains(p))

  def vatRegistered(p: VATRegistered): ResponsiblePeople =
    this.copy(vatRegistered = Some(p), hasChanged = hasChanged || !this.vatRegistered.contains(p))

  def experienceTraining(p: ExperienceTraining): ResponsiblePeople =
    this.copy(experienceTraining = Some(p), hasChanged = hasChanged || !this.experienceTraining.contains(p))

  def training(p: Training): ResponsiblePeople =
    this.copy(training = Some(p), hasChanged = hasChanged || !this.training.contains(p))

  def hasAlreadyPassedFitAndProper(p: Boolean) : ResponsiblePeople =
    this.copy(hasAlreadyPassedFitAndProper = Some(p), hasChanged = hasChanged || !this.hasAlreadyPassedFitAndProper.contains(p))

  def isComplete: Boolean = {
    Logger.debug(s"[ResponsiblePeople][isComplete] $this")
    this match {
      case ResponsiblePeople(
      Some(_), Some(_), Some(_), Some(_),
      Some(pos), None, None, Some(_),
      Some(_), _, _, _, _) if !pos.personalTax => true
      case ResponsiblePeople(
      Some(_), Some(_), Some(_), Some(_),
      Some(_), Some(_), Some(_), Some(_),
      Some(_), _, _, _, _) => true
      case ResponsiblePeople(None, None, None, None, None, None, None, None, None, None, _, _, _) => true
      case _ => false
    }
  }
}

object ResponsiblePeople {

  def anyChanged(newModel: Seq[ResponsiblePeople]): Boolean = {
    newModel exists { _.hasChanged }
  }

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "responsiblepeople"
    val notStarted = Section(messageKey, NotStarted, false, controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
    cache.getEntry[Seq[ResponsiblePeople]](key).fold(notStarted) {
      _.filterNot(_ == ResponsiblePeople()) match {
        case Nil => notStarted
        case model if model forall {
          _.isComplete
        } => Section(messageKey, Completed, anyChanged(model), controllers.responsiblepeople.routes.YourAnswersController.get())
        case model => {
          val index = model.indexWhere { m => !m.isComplete }
          Section(messageKey, Started, anyChanged(model), controllers.responsiblepeople.routes.WhoMustRegisterController.get(index + 1))
        }
      }
    }
  }

  import play.api.libs.json._

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePeople] {
    override def apply(): String = key
  }

  implicit val writes: Writes[ResponsiblePeople] = Json.writes[ResponsiblePeople]

  implicit val reads: Reads[ResponsiblePeople] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "personName").readNullable[PersonName] and
      (__ \ "personResidenceType").readNullable[PersonResidenceType] and
      (__ \ "contactDetails").readNullable[ContactDetails] and
      (__ \ "addressHistory").readNullable[ResponsiblePersonAddressHistory] and
      (__ \ "positions").readNullable[Positions] and
      (__ \ "saRegistered").readNullable[SaRegistered] and
      (__ \ "vatRegistered").readNullable[VATRegistered] and
      (__ \ "experienceTraining").readNullable[ExperienceTraining] and
      (__ \ "training").readNullable[Training] and
      (__ \ "hasAlreadyPassedFitAndProper").readNullable[Boolean] and
      (__ \ "hasChanged").readNullable[Boolean].map {_.getOrElse(false)} and
        (__ \ "lineId").readNullable[Int] and
      (__ \ "status").readNullable[String]
      ) apply ResponsiblePeople.apply _
  }

  implicit def default(responsiblePeople: Option[ResponsiblePeople]): ResponsiblePeople =
    responsiblePeople.getOrElse(ResponsiblePeople())

}
