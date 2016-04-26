package models.responsiblepeople

import play.api.libs.json.Json
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
                             training: Option[Training] = None
                          ) {

  def personName(pn: PersonName): ResponsiblePeople =
    this.copy(personName = Some(pn))

  def personResidenceType(pr: PersonResidenceType): ResponsiblePeople =
    this.copy(personResidenceType = Some(pr))

  def contactDetails(cd: ContactDetails): ResponsiblePeople =
    this.copy(contactDetails = Some(cd))

  def saRegistered(sa: SaRegistered): ResponsiblePeople =
    this.copy(saRegistered = Some(sa))

  def addressHistory(hist: ResponsiblePersonAddressHistory): ResponsiblePeople =
    this.copy(addressHistory = Some(hist))

  def positions(pos: Positions): ResponsiblePeople =
    this.copy(positions = Some(pos))

  def vatRegistered(v: VATRegistered): ResponsiblePeople =
    this.copy(vatRegistered = Some(v))

  def experienceTraining(et: ExperienceTraining): ResponsiblePeople =
    this.copy(experienceTraining = Some(et))

  def training(t: Training): ResponsiblePeople =
    this.copy(training = Some(t))

  def isComplete: Boolean = this match {
    case ResponsiblePeople(
      Some(_), Some(_), Some(_), Some(_),
      Some(pos), None, None, Some(_),
      Some(_)) if !pos.personalTax => true
    case ResponsiblePeople(
      Some(_), Some(_), Some(_), Some(_),
      Some(_), Some(_), Some(_), Some(_),
      Some(_)) => true
    case _ => false
  }
}

object ResponsiblePeople {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "responsiblepeople"
    val notStarted = Section(messageKey, NotStarted, controllers.responsiblepeople.routes.WhoMustRegisterController.get(1))
    val complete = Section(messageKey, Completed, controllers.responsiblepeople.routes.YourAnswersController.get())
    cache.getEntry[Seq[ResponsiblePeople]](key).fold(notStarted) {
      case model if model forall {
        _.isComplete
      } => complete
      case model => {
        val index = model.indexWhere { m => !m.isComplete }
        Section(messageKey, Started, controllers.responsiblepeople.routes.WhoMustRegisterController.get(index + 1))
      }
    }
  }

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePeople] {
    override def apply(): String = key
  }

  implicit val format = Json.format[ResponsiblePeople]

  implicit def default(responsiblePeople: Option[ResponsiblePeople]): ResponsiblePeople =
    responsiblePeople.getOrElse(ResponsiblePeople())

}
