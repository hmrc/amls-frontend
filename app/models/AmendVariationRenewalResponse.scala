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

case class AmendVariationRenewalResponse(
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
                                  ) extends SubmissionResponse {

  override def getRegistrationFee: BigDecimal = registrationFee

  override def getPremiseFeeRate: Option[BigDecimal] = premiseFeeRate

  override def getFpFeeRate: Option[BigDecimal] = fpFeeRate

  override def getFpFee: Option[BigDecimal] = fpFee

  override def getPremiseFee: BigDecimal = premiseFee

  override def getPaymentReference: String = paymentReference.getOrElse("")

  override def getTotalFees: BigDecimal = totalFees
}

object AmendVariationRenewalResponse {

  val key = "AmendVariationResponse"
  implicit val format = Json.format[AmendVariationRenewalResponse]

}
