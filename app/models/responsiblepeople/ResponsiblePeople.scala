package models.responsiblepeople

import typeclasses.MongoKey
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class ResponsiblePeople(addPerson: Option[AddPerson] = None) {

  def addPerson(ap: AddPerson): ResponsiblePeople =
    this.copy(addPerson = Some(ap))

  def isComplete: Boolean =
    this match {
      case ResponsiblePeople(Some(_), Some(_), Some(_)) => true
      case _ => false
    }

}



object ResponsiblePeople {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "responsiblepeople"
    val notStarted = Section(messageKey, NotStarted, controllers.responsiblepeople.routes.WhatYouNeedController.get())
    cache.getEntry[ResponsiblePeople](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.responsiblepeople.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.responsiblepeople.routes.WhatYouNeedController.get())
        }
    }
  }

  import play.api.libs.json._

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePeople] {
    override def apply(): String = "responsible-people"
  }

  implicit val format = Json.format[ResponsiblePeople]

  /*  implicit val reads: Reads[ResponsiblePeople] =  {
  /*
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Reads._
      import play.api.libs.json._
  */
      (
        (__.read[Option[AddPerson]])
      ) (ResponsiblePeople.apply _)
    }

    implicit val writes: Writes[ResponsiblePeople] = Writes[ResponsiblePeople] {
      model =>
        Seq(
          Json.toJson(model.addPerson).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    } */

  implicit def default(responsiblePeople: Option[ResponsiblePeople]): ResponsiblePeople =
    responsiblePeople.getOrElse(ResponsiblePeople())

}