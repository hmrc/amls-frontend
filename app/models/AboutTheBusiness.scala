package models

import play.api.libs.json.Json

object BusinessHasWebsite{
  implicit val formats = Json.format[BusinessHasWebsite]
}

case class BusinessHasWebsite(hasWebsite: Boolean, website: Option[String])
