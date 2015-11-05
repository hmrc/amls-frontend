package models

import play.api.libs.json.Json

object LoginDetails{
  implicit val formats = Json.format[LoginDetails]
}

case class LoginDetails(name: String, password: String)

case class AboutYou(roleWithinBusiness: String)

