package models.renewal

import play.api.libs.json.Json

case class Renewal(involvedInOtherActivities: Option[InvolvedInOther] = None, hasChanged: Boolean = false) {
  def isComplete = {
    this match {
      case Renewal(Some(_), _) => true
      case _ => false
    }
  }
}

object Renewal {
  val key = "renewal"

  implicit val formats = Json.format[Renewal]

}
