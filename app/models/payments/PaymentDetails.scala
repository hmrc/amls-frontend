package models.payments

import models.confirmation.Currency
import play.api.libs.json.Json

case class PaymentDetails(reference: String, amount: Double)

object PaymentDetails {

  val cacheKey = "Payment_Confirmation_Details"

  implicit val formats = Json.format[PaymentDetails]


}
