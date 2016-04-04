package models.governmentgateway

import play.api.libs.json.Json

case class Identifier(
                     `type`: String,
                     value: String
                     )

object Identifier {
  implicit val format = Json.format[Identifier]
}
