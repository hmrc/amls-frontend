package models

import models.supervision.{AnotherBody, ProfessionalBody, ProfessionalBodyMember}
import play.api.libs.json.{Json, Reads}

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

  implicit val reads: Reads[AmendVariationResponse] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "processingDate").read[String] and
        (__ \ "etmpFormBundleNumber").read[String] and
        (__ \ "registrationFee").read[BigDecimal] and
        (__ \ "fpFee").read[BigDecimal].orElse((__ \ "fPFee").read[BigDecimal]).orElse(Reads.pure(None)) and
        (__ \ "premiseFee").read[BigDecimal] and
        (__ \ "totalFees").read[BigDecimal] and
        (__ \ "paymentReference").readNullable[String] and
        (__ \ "difference").readNullable[BigDecimal] and
        (__ \ "addedResponsiblePeople").read[Int] and
        (__ \ "addedResponsiblePeopleFitAndProper").read[Int] and
        (__ \ "addedFullYearTradingPremises").read[Int] and
        (__ \ "halfYearlyTradingPremises").read[Int] and
        (__ \ "zeroRatedTradingPremises").read[Int]
      ) apply AmendVariationResponse.apply _
  }
}
