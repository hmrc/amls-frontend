package models

import play.api.libs.json.Json

object EmployedWithinTheBusinessModel {
  implicit val formats = Json.format[EmployedWithinTheBusinessModel]
}

case class EmployedWithinTheBusinessModel(isEmployed: Boolean)
