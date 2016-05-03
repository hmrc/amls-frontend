package models.tcsp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Tcsp (
                  tcspTypes: Option[TcspTypes] = None
               ) {

  def tcspTypes(trust: TcspTypes) : Tcsp =
    this.copy(tcspTypes = Some(trust))

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

  implicit val format = Json.format[Tcsp]

  implicit def default(details: Option[Tcsp]): Tcsp =
    details.getOrElse(Tcsp())
}
