package models

import play.api.libs.json.Json


case class SubscriptionResponse(
                                 etmpFormBundleNumber: String,
                                 amlsRefNo: String,
                                 registrationFee: BigDecimal,
                                 fpFee: Option[BigDecimal],
                                 fpFeeRate: Option[BigDecimal],
                                 premiseFee: BigDecimal,
                                 premiseFeeRate: Option[BigDecimal],
                                 totalFees: BigDecimal,
                                 paymentReference: String
                               ) extends SubmissionResponse

object SubscriptionResponse {

  val key = "Subscription"

  implicit val format = Json.format[SubscriptionResponse]
}
