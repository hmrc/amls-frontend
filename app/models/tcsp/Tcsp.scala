package models.tcsp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Tcsp() {

  def isComplete: Boolean = this match {
    case Tcsp() => true
    case _ => false
  }

}

object Tcsp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tcsp"
    val notStarted = Section(messageKey, NotStarted, controllers.tcsp.routes.WhatYouNeedController.get())
    //TODO: Uncomment this once a sub-section has been added. Json can not format empty case class.
//    cache.getEntry[Tcsp](key).fold(notStarted) {
//      model =>
//        if (model.isComplete) {
//          Section(messageKey, Completed, controllers.routes.RegistrationProgressController.get())
//        } else {
//          //TODO: Update this route to correct page.
//          Section(messageKey, Started, controllers.routes.RegistrationProgressController.get())
//        }
//    }
    //TODO: Delete this once above is done.
    notStarted
  }

  val key = "tcsp"

  implicit val mongoKey = new MongoKey[Tcsp] {
    override def apply(): String = "tcsp"
  }

//  implicit val reads: Reads[Tcsp] = ???
//
//  implicit val writes: Writes[Tcsp] = ???

  implicit def default(details: Option[Tcsp]): Tcsp =
    details.getOrElse(Tcsp())
}
