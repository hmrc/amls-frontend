package models.tcsp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Tcsp(providedServices: Option[ProvidedServices] = None) {

  def providedServices(ps: ProvidedServices): Tcsp = this.copy(providedServices = Some(ps))

  def isComplete: Boolean = this match {
    case Tcsp(Some(_)) => true
    case _ => false
  }

}

object Tcsp {

  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tcsp"
    val notStarted = Section(messageKey, NotStarted, controllers.tcsp.routes.WhatYouNeedController.get())
    cache.getEntry[Tcsp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.routes.RegistrationProgressController.get())
        } else {
          Section(messageKey, Started, controllers.tcsp.routes.WhatYouNeedController.get())
        }
    }
  }

  val key = "tcsp"

  implicit val mongoKey = new MongoKey[Tcsp] {
    override def apply(): String = "tcsp"
  }

  implicit val reads: Reads[Tcsp] = (
    __.read[Option[ProvidedServices]]
    ) (Tcsp.apply _)

  implicit val writes: Writes[Tcsp] =
    Writes[Tcsp] {
      model =>
        Seq(
          Json.toJson(model.providedServices).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(tcsp: Option[Tcsp]): Tcsp =
    tcsp.getOrElse(Tcsp())
}
