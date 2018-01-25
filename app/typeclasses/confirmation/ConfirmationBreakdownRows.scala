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

import models.SubmissionResponse
import models.businessmatching.BusinessActivities
import models.confirmation.BreakdownRow
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import services.FeeCalculations
import ResponsePeopleRowsInstances._

trait ConfirmationBreakdownRows[A] extends FeeCalculations{
  def apply(
             value: A,
             premises: Seq[TradingPremises],
             people: Seq[ResponsiblePeople],
             businessActivities: BusinessActivities,
             subQuantity: Int
           ): Seq[BreakdownRow]

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
        ) ++ ResponsePeopleRows[SubmissionResponse](subscription, people, businessActivities.businessActivities) ++ Seq(
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