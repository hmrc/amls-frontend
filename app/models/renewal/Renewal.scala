package models.renewal

import play.api.libs.json.Json

case class Renewal(involedInOtherActivities: Option[InvolvedInOther] = None, hasChanged: Boolean = false) {
  def isComplete = true
}

object Renewal {
  val key = "renewal"

  implicit val formats = Json.format[Renewal]

}
