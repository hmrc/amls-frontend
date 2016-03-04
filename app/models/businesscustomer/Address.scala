package models.businesscustomer

import play.api.libs.json.Json

case class Address(
                  line_1: String,
                  line_2: String,
                  line_3: Option[String],
                  line_4: Option[String],
                  postcode: Option[String],
                  country: String
                  )

object Address {
  implicit val format = Json.format[Address]
}
