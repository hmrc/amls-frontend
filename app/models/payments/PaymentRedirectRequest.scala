package models.payments

import play.api.libs.json.Json

case class PaymentRedirectRequest(reference: String, amount: Double, redirectUrl: String)

object PaymentRedirectRequest {

  implicit val formats = Json.format[PaymentRedirectRequest]

}
