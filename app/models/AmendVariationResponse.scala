package models

import play.api.libs.json.Json

trait SubmissionResponse {
  val registrationFee: BigDecimal
  val fpFee: Option[BigDecimal]
}

case class AmendVariationResponse (
                                    processingDate: String,
                                    etmpFormBundleNumber: String,
                                    registrationFee: BigDecimal,
                                    fpFee: Option[BigDecimal],
                                    premiseFee: BigDecimal,
                                    totalFees: BigDecimal,
                                    paymentReference: Option[String],
                                    difference: Option[BigDecimal]
                                  ) extends SubmissionResponse

object AmendVariationResponse {

  val key = "AmendVariationResponse"
  implicit val format = Json.format[AmendVariationResponse]
}
