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

package typeclasses

import models.{SubmissionResponse, SubscriptionResponse}
import models.businessmatching.{BusinessActivities, BusinessActivity, TrustAndCompanyServices, MoneyServiceBusiness => MSB}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import services.FeeCalculations

trait ConfirmationBreakdownRows[A] extends FeeCalculations{
  def apply(
             value: A,
             premises: Seq[TradingPremises],
             people: Seq[ResponsiblePeople],
             businessActivities: BusinessActivities,
             subQuantity: Int
           ): Seq[BreakdownRow]


  def responsiblePeopleRows
  (people: Seq[ResponsiblePeople],
   subscription: SubmissionResponse,
   activities: Set[BusinessActivity]): Seq[BreakdownRow] = {
    if (showBreakdown(subscription.getFpFee, activities)) {

      splitPeopleByFitAndProperTest(people) match {
        case (passedFP, notFP) =>
          Seq(
            BreakdownRow(peopleRow(subscription).message, notFP.size, peopleRow(subscription).feePer, Currency.fromBD(subscription.getFpFee.getOrElse(0)))
          ) ++ (if (passedFP.nonEmpty) {
            Seq(
              BreakdownRow(peopleFPPassed.message, passedFP.size, max(0, peopleFPPassed.feePer), Currency.fromBD(max(0, peopleFPPassed.feePer)))
            )
          } else {
            Seq.empty
          })
      }
    } else {
      Seq.empty
    }
  }

  private val showBreakdown = (fpFee: Option[BigDecimal], activities: Set[BusinessActivity]) =>
    fpFee.fold(activities.exists(act => act == MSB || act == TrustAndCompanyServices)) { _ => true }

  private val splitPeopleByFitAndProperTest = (people: Seq[ResponsiblePeople]) =>
    ResponsiblePeople.filter(people).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false))

  private val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

}

object BreakdownRowInstances {

  implicit val breakdownRowFromSubscription: ConfirmationBreakdownRows[SubmissionResponse] = {
    new ConfirmationBreakdownRows[SubmissionResponse] {
      def apply(
                 subscription: SubmissionResponse,
                 premises: Seq[TradingPremises],
                 people: Seq[ResponsiblePeople],
                 businessActivities: BusinessActivities,
                 subQuantity: Int
               ): Seq[BreakdownRow] = {
        Seq(
          BreakdownRow(submissionRow(subscription).message, subQuantity, submissionRow(subscription).feePer, subQuantity * submissionRow(subscription).feePer)
        ) ++ responsiblePeopleRows(people, subscription, businessActivities.businessActivities) ++ Seq(
          BreakdownRow(premisesRow(subscription).message, premises.size, premisesRow(subscription).feePer, subscription.getPremiseFee)
        )
      }
    }
  }

}

object BreakdownRows {

  def generateBreakdownRows[A](
                                value: A,
                                premises: Seq[TradingPremises],
                                people: Seq[ResponsiblePeople],
                                businessActivities: BusinessActivities,
                                subQuantity: Int
                              )(implicit b: ConfirmationBreakdownRows[A]): Seq[BreakdownRow] = b(value, premises, people, businessActivities, subQuantity)

}