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

import models.{AmendVariationRenewalResponse, SubmissionResponse}
import models.businessmatching.{BusinessActivities, BusinessActivity, TrustAndCompanyServices, MoneyServiceBusiness => MSB}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import services.{FeeCalculations, RowEntity}
import ResponsePeopleRowsInstances._

trait ConfirmationBreakdownRows[A] extends FeeCalculations {
  def apply(
             value: A,
             businessActivities: Option[BusinessActivities],
             premises: Option[Seq[TradingPremises]],
             people: Option[Seq[ResponsiblePeople]]
           ): Seq[BreakdownRow]

  def subscriptionQuantity(subscription: SubmissionResponse): Int =
    if (subscription.getRegistrationFee == 0) 0 else 1

  def tradingPremisesVariationRows(variationRenewalResponse: AmendVariationRenewalResponse): Seq[BreakdownRow] = {
    val breakdownRows = Seq.empty

    def variationRow(count: Int, rowEntity: RowEntity, total: AmendVariationRenewalResponse => BigDecimal): Seq[BreakdownRow] = {
      if (count > 0) {
        breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(variationRenewalResponse))))
      } else {
        Seq.empty
      }
    }

    def tpFullYearRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.addedFullYearTradingPremises,
      premisesVariationRow(variationRenewalResponse),
      fullPremisesFee
    )

    def tpHalfYearRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.halfYearlyTradingPremises,
      premisesHalfYear(variationRenewalResponse),
      renewalHalfYearPremisesFee
    )

    def tpZeroRow: Seq[BreakdownRow] = variationRow(
      variationRenewalResponse.zeroRatedTradingPremises,
      PremisesZero,
      renewalZeroPremisesFee
    )

    tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow
  }
}

object BreakdownRowInstances {

  implicit val breakdownRowFromSubscription: ConfirmationBreakdownRows[SubmissionResponse] = {
    new ConfirmationBreakdownRows[SubmissionResponse] {
      def apply(
                 subscription: SubmissionResponse,
                 businessActivities: Option[BusinessActivities],
                 premises: Option[Seq[TradingPremises]],
                 people: Option[Seq[ResponsiblePeople]]
               ): Seq[BreakdownRow] = {

        businessActivities match {
          case Some(activities) if people.isDefined =>

            val subQuantity = subscriptionQuantity(subscription)
            val registrationFeeRow = submissionRow(subscription)

            Seq(
              BreakdownRow(registrationFeeRow.message, subQuantity, registrationFeeRow.feePer, subQuantity * registrationFeeRow.feePer)
            ) ++ ResponsePeopleRows[SubmissionResponse](subscription, activities.businessActivities, people) ++ Seq(
              BreakdownRow(premisesRow(subscription).message, premises.size, premisesRow(subscription).feePer, subscription.getPremiseFee)
            )
          case _ => Seq.empty[BreakdownRow]
        }
      }
    }
  }

  implicit val breakdownRowFromVariation: ConfirmationBreakdownRows[AmendVariationRenewalResponse] = {
    new ConfirmationBreakdownRows[AmendVariationRenewalResponse] {
      override def apply(
                          value: AmendVariationRenewalResponse,
                          businessActivities: Option[BusinessActivities],
                          premises: Option[Seq[TradingPremises]],
                          people: Option[Seq[ResponsiblePeople]]) = {

        businessActivities match {
          case Some(activities) =>
            ResponsePeopleRows[AmendVariationRenewalResponse](value, activities.businessActivities, None) ++ tradingPremisesVariationRows(value)
          case _ =>
            val breakdownRows = Seq.empty

            def renewalRow(count: Int, rowEntity: RowEntity, total: AmendVariationRenewalResponse => BigDecimal): Seq[BreakdownRow] = {
              if (count > 0) {
                breakdownRows ++ Seq(BreakdownRow(rowEntity.message, count, rowEntity.feePer, Currency(total(value))))
              } else {
                Seq.empty
              }
            }

            def rpRow: Seq[BreakdownRow] = renewalRow(value.addedResponsiblePeople, peopleVariationRow(value), renewalPeopleFee)

            def fpRow: Seq[BreakdownRow] = renewalRow(value.addedResponsiblePeopleFitAndProper, peopleFPPassed, renewalFitAndProperDeduction)

            def tpFullYearRow: Seq[BreakdownRow] = renewalRow(value.addedFullYearTradingPremises, premisesVariationRow(value), fullPremisesFee)

            def tpHalfYearRow: Seq[BreakdownRow] = renewalRow(value.halfYearlyTradingPremises, premisesHalfYear(value), renewalHalfYearPremisesFee)

            def tpZeroRow: Seq[BreakdownRow] = renewalRow(value.zeroRatedTradingPremises, PremisesZero, renewalZeroPremisesFee)

            rpRow ++ fpRow ++ tpZeroRow ++ tpHalfYearRow ++ tpFullYearRow
        }

      }
    }
  }

}

object BreakdownRows {

  def generateBreakdownRows[A](
                                value: A,
                                businessActivities: Option[BusinessActivities],
                                premises: Option[Seq[TradingPremises]],
                                people: Option[Seq[ResponsiblePeople]]
                              )(implicit b: ConfirmationBreakdownRows[A]): Seq[BreakdownRow] = b(value, businessActivities, premises, people)

}