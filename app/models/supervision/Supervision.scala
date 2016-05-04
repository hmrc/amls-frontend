package models.supervision

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Supervision(anotherBody: Option[AnotherBody] = None) {

  def anotherBody(anotherBody: AnotherBody): Supervision =
    this.copy(anotherBody = Some(anotherBody))

  def isComplete: Boolean = this match {
    case Supervision(Some(_)) => true
    case _ => false
  }

}

object Supervision {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "supervision"
    val notStarted = Section(messageKey, NotStarted, controllers.routes.RegistrationProgressController.get())
    cache.getEntry[Supervision](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.routes.RegistrationProgressController.get())
        } else {
          Section(messageKey, Started, controllers.routes.RegistrationProgressController.get())
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
