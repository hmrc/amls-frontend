package models.supervision

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Supervision(
                        anotherBody: Option[AnotherBody] = None,
                        professionalBodyMember: Option[ProfessionalBodyMember] = None,
                        professionalBody: Option[ProfessionalBody] = None,
                        hasChanged: Boolean = false) {

  def anotherBody(p: AnotherBody): Supervision =
    this.copy(anotherBody = Some(p), hasChanged = hasChanged || !this.anotherBody.contains(p))

  def professionalBodyMember(p: ProfessionalBodyMember): Supervision =
    this.copy(professionalBodyMember = Some(p), hasChanged = hasChanged || !this.professionalBodyMember.contains(p))

  def professionalBody(p: ProfessionalBody): Supervision =
    this.copy(professionalBody = Some(p), hasChanged = hasChanged || !this.professionalBody.contains(p))

  def isComplete: Boolean = this match {
    case Supervision(Some(_), Some(_), Some(_), _) => true
    case _ => false
  }

}

object Supervision {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "supervision"
    val notStarted = Section(messageKey, NotStarted, false, controllers.supervision.routes.WhatYouNeedController.get())
    cache.getEntry[Supervision](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.supervision.routes.SummaryController.get(true))
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.supervision.routes.WhatYouNeedController.get())
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
