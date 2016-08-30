package models.asp

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Asp(
              services: Option[ServicesOfBusiness] = None,
              otherBusinessTaxMatters: Option[OtherBusinessTaxMatters] = None

              ) {

  def services(p: ServicesOfBusiness): Asp =
    this.copy(services = Some(p))

  def otherBusinessTaxMatters(p: OtherBusinessTaxMatters): Asp =
    this.copy(otherBusinessTaxMatters = Some(p))

  def isComplete: Boolean = this match {
      case Asp(Some(_), Some(_)) => true
      case _ => false
  }
}

object Asp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "asp"
    val notStarted = Section(messageKey, NotStarted, false, controllers.asp.routes.WhatYouNeedController.get())
    cache.getEntry[Asp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, false, controllers.asp.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, false, controllers.asp.routes.WhatYouNeedController.get())
        }
    }
  }

  val key = "asp"

  implicit val mongoKey = new MongoKey[Asp] {
    override def apply(): String = "asp"
  }

  implicit val format = Json.format[Asp]

  implicit def default(details: Option[Asp]): Asp =
    details.getOrElse(Asp())
}
