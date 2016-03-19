package models

import play.api.libs.json.Json

case class SubscriptionResponse(
                                etmpFormBundleNumber: String,
                                amlsRefNo: String,
                                registrationFee: BigDecimal,
                                fpFee: Option[BigDecimal],
                                premiseFee: BigDecimal,
                                totalFees: BigDecimal,
                                paymentReference: String
                              )

object SubscriptionResponse {
  implicit val format = Json.format[SubscriptionResponse]
}