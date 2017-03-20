package models.renewal

import play.api.libs.json.Json

case class Renewal
(
  involvedInOtherActivities: Option[InvolvedInOther] = None,
  hasChanged: Boolean = false
)
{
  def isComplete = {
    this match {
      case Renewal(Some(_), _) => true
      case _ => false
    }
  }

  def involvedInOtherActivities(model: InvolvedInOther): Renewal =
    this.copy(involvedInOtherActivities = Some(model), hasChanged = hasChanged || this.involvedInOtherActivities != Some(model))

}

object Renewal {
  val key = "renewal"

  implicit val formats = Json.format[Renewal]

}
