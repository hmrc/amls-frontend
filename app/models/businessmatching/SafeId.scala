package models.businessmatching

import play.api.libs.json.Json

case class SafeId(value: String)

object SafeId {
  implicit val format = Json.format[SafeId]
}
