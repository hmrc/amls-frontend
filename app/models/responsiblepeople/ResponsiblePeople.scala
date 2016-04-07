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
    val notStarted = Section(messageKey, NotStarted, controllers.responsiblepeople.routes.WhatYouNeedController.get())
    val complete = Section(messageKey, Completed, controllers.bankdetails.routes.SummaryController.get())
    cache.getEntry[Seq[ResponsiblePeople]](key).fold(notStarted) {
      case model if model.isEmpty => complete
      case model if model forall { _.isComplete } => complete
      case model =>
        val index = model.indexWhere {
          case model if !model.isComplete => true
          case _ => false
        }
        Section(messageKey, Started, controllers.responsiblepeople.routes.WhatYouNeedController.get())
    }
  }

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePeople] {
    override def apply(): String = key
  }

  implicit val format = Json.format[ResponsiblePeople]
  /*import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val reads: Reads[ResponsiblePeople] = (
    __.read[Option[AddPerson]] and
      __.read[Option[PersonResidenceType]] and
      __.read[Option[SaRegistered]]
    ) (ResponsiblePeople.apply _)

  implicit val writes: Writes[ResponsiblePeople] = Writes[ResponsiblePeople] {
    model =>
      Seq(
        Json.toJson(model.addPerson).asOpt[JsObject],
        Json.toJson(model.personResidenceType).asOpt[JsObject],
        Json.toJson(model.saRegistered).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }*/

  implicit def default(responsiblePeople: Option[ResponsiblePeople]): ResponsiblePeople =
    responsiblePeople.getOrElse(ResponsiblePeople())

}