package models.supervision

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Supervision(anotherBody: Option[AnotherBody] = None,
                       professionalBody: Option[ProfessionalBody] = None) {

  def anotherBody(anotherBody: AnotherBody): Supervision =
    this.copy(anotherBody = Some(anotherBody))

  def professionalBody(p: ProfessionalBody): Supervision =
    this.copy(professionalBody = Some(p))

  def isComplete: Boolean = this match {
    case Supervision(Some(_), Some(_)) => true
    case _ => false
  }

}

object Supervision {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "supervision"
    val notStarted = Section(messageKey, NotStarted, controllers.supervision.routes.WhatYouNeedController.get())
    cache.getEntry[Supervision](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.supervision.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.supervision.routes.WhatYouNeedController.get())
        }
    }
  }

  import play.api.libs.json._

  val key = "supervision"

  implicit val mongoKey = new MongoKey[Supervision] {
    override def apply(): String = "supervision"
  }

  implicit val formats = Json.format[Supervision]

  implicit def default(supervision: Option[Supervision]): Supervision =
    supervision.getOrElse(Supervision())
}
