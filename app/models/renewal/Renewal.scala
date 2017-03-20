package models.renewal

import play.api.libs.json.Json

case class Renewal(hasChanged: Boolean) {
  def isComplete = true
}

object Renewal {
  val key = "renewal"

  implicit val formats = Json.format[Renewal]

}
