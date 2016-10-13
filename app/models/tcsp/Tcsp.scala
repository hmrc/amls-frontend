package models.tcsp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Tcsp (tcspTypes: Option[TcspTypes] = None,
                 providedServices: Option[ProvidedServices] = None,
                 servicesOfAnotherTCSP: Option[ServicesOfAnotherTCSP] = None,
                 hasChanged:Boolean = false) {

  def tcspTypes(trust: TcspTypes): Tcsp =
    this.copy(tcspTypes = Some(trust), hasChanged = hasChanged || !this.tcspTypes.contains(trust))

  def providedServices(ps: ProvidedServices): Tcsp =
    this.copy(providedServices = Some(ps), hasChanged = hasChanged || !this.providedServices.contains(ps))

  def servicesOfAnotherTCSP(p: ServicesOfAnotherTCSP): Tcsp =
    this.copy(servicesOfAnotherTCSP = Some(p), hasChanged = hasChanged || !this.servicesOfAnotherTCSP.contains(p))

  def isComplete: Boolean = this match {
    case Tcsp(Some(_), Some(_), Some(_), _) => true
    case Tcsp(Some(TcspTypes(serviceProviders)), _, Some(_), _) if !serviceProviders.contains(RegisteredOfficeEtc) => true
    case _ => false
  }
}

object Tcsp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tcsp"
    val notStarted = Section(messageKey, NotStarted, false, controllers.tcsp.routes.WhatYouNeedController.get())
    cache.getEntry[Tcsp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.tcsp.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.tcsp.routes.WhatYouNeedController.get())
        }
    }
  }

  val key = "tcsp"

  implicit val mongoKey = new MongoKey[Tcsp] {
    override def apply(): String = "tcsp"
  }

  implicit val jsonWrites = Json.writes[Tcsp]

  implicit val jsonReads : Reads[Tcsp] = {
    (__ \ "tcspTypes").readNullable[TcspTypes] and
      (__ \ "providedServices").readNullable[ProvidedServices] and
      (__ \ "servicesOfAnotherTCSP").readNullable[ServicesOfAnotherTCSP] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(Tcsp.apply _)
  implicit def default(tcsp: Option[Tcsp]): Tcsp =
    tcsp.getOrElse(Tcsp())
}
