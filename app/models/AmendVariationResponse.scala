package models

import play.api.libs.json.Json

case class AmendVariationResponse (
                                    processingDate: String,
                                    etmpFormBundleNumber: String,
                                    registrationFee: BigDecimal,
                                    fpFee: Option[BigDecimal],
                                    premiseFee: BigDecimal,
                                    totalFees: BigDecimal,
                                    paymentReference: Option[String],
                                    difference: Option[BigDecimal],
                                    addedResponsiblePeople: Int = 0,
                                    addedResponsiblePeopleFitAndProper: Int = 0,
                                    addedFullYearTradingPremises: Int = 0,
                                    halfYearlyTradingPremises: Int = 0,
                                    zeroRatedTradingPremises: Int = 0
                                  ) extends SubmissionResponse

object AmendVariationResponse {

  val key = "AmendVariationResponse"
  implicit val format = Json.format[AmendVariationResponse]
}
