package models.renewal

import models.SubmissionResponse
import play.api.libs.json.{Json, Reads}

case class RenewalResponse (
                                    processingDate: String,
                                    etmpFormBundleNumber: String,
                                    registrationFee: BigDecimal,
                                    fpFee: Option[BigDecimal],
                                    fpFeeRate: Option[BigDecimal],
                                    premiseFee: BigDecimal,
                                    premiseFeeRate: Option[BigDecimal],
                                    totalFees: BigDecimal,
                                    paymentReference: Option[String],
                                    difference: Option[BigDecimal],
                                    addedResponsiblePeople: Int = 0,
                                    addedResponsiblePeopleFitAndProper: Int = 0,
                                    addedFullYearTradingPremises: Int = 0,
                                    halfYearlyTradingPremises: Int = 0,
                                    zeroRatedTradingPremises: Int = 0
                                  ) extends SubmissionResponse

object RenewalResponse {

  val key = "RenewalResponse"
  implicit val format = Json.format[RenewalResponse]

  implicit val reads: Reads[RenewalResponse] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "processingDate").read[String] and
        (__ \ "etmpFormBundleNumber").read[String] and
        (__ \ "registrationFee").read[BigDecimal] and
        (__ \ "fpFee").read(Reads.optionWithNull[BigDecimal]).orElse((__ \ "fPFee").read(Reads.optionWithNull[BigDecimal])).orElse(Reads.pure(None)) and
        (__ \ "fpFeeRate").readNullable[BigDecimal] and
        (__ \ "premiseFee").read[BigDecimal] and
        (__ \ "premiseFeeRate").readNullable[BigDecimal] and
        (__ \ "totalFees").read[BigDecimal] and
        (__ \ "paymentReference").readNullable[String] and
        (__ \ "difference").readNullable[BigDecimal] and
        (__ \ "addedResponsiblePeople").read[Int] and
        (__ \ "addedResponsiblePeopleFitAndProper").read[Int] and
        (__ \ "addedFullYearTradingPremises").read[Int] and
        (__ \ "halfYearlyTradingPremises").read[Int] and
        (__ \ "zeroRatedTradingPremises").read[Int]
      ) apply RenewalResponse.apply _
  }
}
