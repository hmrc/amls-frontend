package models.payments

import play.api.libs.json.Json

case class PaymentDetails(reference: String, amount: Float)

object PaymentDetails {

  implicit val formats = Json.format[PaymentDetails]

}
