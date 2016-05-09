package models.tcsp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Tcsp (tcspTypes: Option[TcspTypes] = None,
                 providedServices: Option[ProvidedServices] = None,
                 servicesOfAnotherTCSP: Option[ServicesOfAnotherTCSP] = None) {

  def tcspTypes(trust: TcspTypes): Tcsp =
    this.copy(tcspTypes = Some(trust))

  def providedServices(ps: ProvidedServices): Tcsp =
    this.copy(providedServices = Some(ps))

  def servicesOfAnotherTCSP(p: ServicesOfAnotherTCSP): Tcsp =
    this.copy(servicesOfAnotherTCSP = Some(p))

  def isComplete: Boolean = this match {
    case Tcsp(Some(_), Some(_), Some(_)) => true
    case _ => false
  }

}

object Tcsp {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tcsp"
    val notStarted = Section(messageKey, NotStarted, controllers.tcsp.routes.WhatYouNeedController.get())
    cache.getEntry[Tcsp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.tcsp.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.tcsp.routes.WhatYouNeedController.get())
        }
    }
  }

  import play.api.libs.json._

  val key = "tcsp"

  implicit val mongoKey = new MongoKey[Tcsp] {
    override def apply(): String = "tcsp"
  }

  implicit val formats = Json.format[Tcsp]

  implicit def default(tcsp: Option[Tcsp]): Tcsp =
    tcsp.getOrElse(Tcsp())
}
