package models.msb

import models.asp.Asp
import models.registrationprogress.{Started, Completed, NotStarted, Section}
import play.api.libs.json.Json
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Msb(
                name: Option[String] = None
              ) {
  def isComplete: Boolean = this match {
    case Msb(Some(_)) => true
    case _ => false
  }
}

object Msb {

  val key = "msb"

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "msb"
    val notStarted = Section(messageKey, NotStarted, controllers.msb.routes.WhatYouNeedController.get())
    cache.getEntry[Msb](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.msb.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.msb.routes.WhatYouNeedController.get())
        }
    }
  }

  implicit val mongoKey = new MongoKey[Asp] {
    override def apply(): String = "msb"
  }

  implicit val format = Json.format[Msb]

  implicit def default(details: Option[Msb]): Msb =
    details.getOrElse(Msb())
}

