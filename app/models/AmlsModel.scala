package models

import play.api.libs.json.Json

case class LoginDetails(name: String, password: String)

object LoginDetails {
  implicit val formats = Json.format[LoginDetails]
}
