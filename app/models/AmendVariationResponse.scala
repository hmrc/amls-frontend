/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json.{Json, Reads}

case class AmendVariationResponse (
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
      ) apply AmendVariationResponse.apply _
  }
}
