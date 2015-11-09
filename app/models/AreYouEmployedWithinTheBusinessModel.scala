package models

import play.api.libs.json.Json

object AreYouEmployedWithinTheBusinessModel {
  implicit val formats = Json.format[AreYouEmployedWithinTheBusinessModel]
}

case class AreYouEmployedWithinTheBusinessModel(yesNo: String)
