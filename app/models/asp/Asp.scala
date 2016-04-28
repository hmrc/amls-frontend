package models.asp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Asp() {

  def isComplete: Boolean = this match {
      case Asp() => true
      case _ => false
  }

}

object Asp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "asp"
    val notStarted = Section(messageKey, NotStarted, controllers.asp.routes.WhatYouNeedController.get())
    cache.getEntry[Asp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          //TODO: Update this route to correct page.
          Section(messageKey, Completed, controllers.routes.RegistrationProgressController.get())
        } else {
          //TODO: Update this route to correct page.
          Section(messageKey, Started, controllers.routes.RegistrationProgressController.get())
        }
    }
  }

  val key = "asp"

  implicit val mongoKey = new MongoKey[Asp] {
    override def apply(): String = "asp"
  }

  // TODO: Update this with actual code
  implicit val reads: Reads[Asp] =
    Reads(_ => JsSuccess(Asp()))

  // TODO: Update this with actual code
  implicit val writes: Writes[Asp] =
    Writes(_ => Json.obj() )

  implicit def default(details: Option[Asp]): Asp =
    details.getOrElse(Asp())
}
