/*
 * Copyright 2018 HM Revenue & Customs
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

package typeclasses.confirmation

import config.ApplicationConfig
import models.{AmendVariationRenewalResponse, SubmissionResponse}
import services.RowEntity

trait FeeCalculations {

  def submissionRow(response: SubmissionResponse) = RowEntity("confirmation.submission", response.getRegistrationFee)

  def premisesRow(response: SubmissionResponse) = RowEntity("confirmation.tradingpremises",
    response.getPremiseFeeRate.getOrElse(ApplicationConfig.premisesFee))

  def premisesVariationRow(variationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.tradingpremises",
    variationResponse.getPremiseFeeRate.getOrElse(ApplicationConfig.premisesFee))

  def premisesHalfYear(response: SubmissionResponse) = RowEntity("confirmation.tradingpremises.half",
    premisesRow(response).feePer / 2)

  def renewalPremisesHalfYear(rvariationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.tradingpremises.half",
    premisesVariationRow(rvariationResponse).feePer / 2)

  val PremisesZero = RowEntity("confirmation.tradingpremises.zero", 0)

  def peopleRow(response: SubmissionResponse) = RowEntity("confirmation.responsiblepeople",
    response.getFpFeeRate.getOrElse(ApplicationConfig.peopleFee))

  def peopleVariationRow(variationResponse: AmendVariationRenewalResponse) = RowEntity("confirmation.responsiblepeople",
    variationResponse.getFpFeeRate.getOrElse(ApplicationConfig.peopleFee))

  def renewalTotalPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    (premisesRow(renewal).feePer * renewal.addedFullYearTradingPremises) + renewalHalfYearPremisesFee(renewal)

  def fullPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    premisesVariationRow(renewal).feePer * renewal.addedFullYearTradingPremises

  def renewalHalfYearPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    renewalPremisesHalfYear(renewal).feePer * renewal.halfYearlyTradingPremises

  def renewalPeopleFee(renewal: AmendVariationRenewalResponse): BigDecimal =
    peopleVariationRow(renewal).feePer * renewal.addedResponsiblePeople

  def renewalFitAndProperDeduction(renewal: AmendVariationRenewalResponse): BigDecimal = 0

  def renewalZeroPremisesFee(renewal: AmendVariationRenewalResponse): BigDecimal = 0

  val peopleFPPassed = RowEntity("confirmation.responsiblepeople.fp.passed", 0)

  val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

}