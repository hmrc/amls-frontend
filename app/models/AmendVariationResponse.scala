package models

import play.api.libs.json.Json

case class AmendVariationResponse(
                                   processingDate: String,
                                   etmpFormBundleNumber: String,
                                   registrationFee: BigDecimal,
                                   fpFee: Option[BigDecimal],
                                   premiseFee: BigDecimal,
                                   totalFees: BigDecimal,
                                   paymentReference: Option[String],
                                   difference: Option[BigDecimal]
                                 )

object AmendVariationResponse {

  val key = "AmendVariationResponse"
  implicit val format = Json.format[AmendVariationResponse]
}
