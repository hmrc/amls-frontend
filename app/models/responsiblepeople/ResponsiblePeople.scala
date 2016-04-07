package models.responsiblepeople

import play.api.libs.json.Json
import typeclasses.MongoKey
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class ResponsiblePeople(addPerson: Option[AddPerson] = None,
                             personResidenceType: Option[PersonResidenceType] = None,
                             saRegistered: Option[SaRegistered] = None
                             ) {

  def addPerson(ap: AddPerson): ResponsiblePeople =
    this.copy(addPerson = Some(ap))

  def personResidenceType(pr: PersonResidenceType): ResponsiblePeople =
    this.copy(personResidenceType = Some(pr))

  def saRegistered(sa: SaRegistered): ResponsiblePeople =
    this.copy(saRegistered = Some(sa))

  def isComplete: Boolean =
    this match {
      case ResponsiblePeople(_, _, _) => true
      case _ => false
    }
}

object ResponsiblePeople {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "responsiblepeople"
    val notStarted = Section(messageKey, NotStarted, controllers.responsiblepeople.routes.WhatYouNeedController.get(1))
    cache.getEntry[ResponsiblePeople](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.responsiblepeople.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.responsiblepeople.routes.WhatYouNeedController.get(1))
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