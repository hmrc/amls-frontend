package models.renewal

import play.api.libs.json.Json

case class Renewal
(
  involvedInOtherActivities: Option[InvolvedInOther] = None,
  turnover: Option[AMLSTurnover] = None,
  hasChanged: Boolean = false
)
{
  def isComplete = {
    this match {
      case Renewal(Some(_), Some(_), _) => true
      case _ => false
    }
  }

  def involvedInOtherActivities(model: InvolvedInOther): Renewal =
    this.copy(involvedInOtherActivities = Some(model), hasChanged = hasChanged || this.involvedInOtherActivities.contains(model))

  def turnover(model: AMLSTurnover): Renewal =
    this.copy(turnover = Some(model), hasChanged = hasChanged || this.turnover.contains(model))

}

object Renewal {
  val key = "renewal"

  implicit val formats = Json.format[Renewal]

  implicit def default(renewal: Option[Renewal]): Renewal =
    renewal.getOrElse(Renewal())

}
