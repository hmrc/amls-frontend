package models.businesscustomer

import models.Country
import play.api.libs.json.Json

case class Address(
                  line_1: String,
                  line_2: String,
                  line_3: Option[String],
                  line_4: Option[String],
                  postcode: Option[String],
                  country: Country
                  ) {
  def toLines: Seq[String] = {
      Seq(
        Some(line_1),
        Some(line_2),
        line_3,
        line_4,
        postcode,
        Some(country.toString)
      ).flatten
  }
}

object Address {
  implicit val format = Json.format[Address]
}
