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
import models.businessmatching.{BusinessActivity, TrustAndCompanyServices, MoneyServiceBusiness => MSB}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePeople
import services.FeeCalculations

trait ResponsePeopleRows[A] extends FeeCalculations {
  def apply(
             value: A,
             people: Seq[ResponsiblePeople],
             activities: Set[BusinessActivity]
           ): Seq[BreakdownRow]

  val showBreakdown = (fpFee: Option[BigDecimal], activities: Set[BusinessActivity]) =>
    fpFee.fold(activities.exists(act => act == MSB || act == TrustAndCompanyServices)) { _ => true }

  val splitPeopleByFitAndProperTest = (people: Seq[ResponsiblePeople]) =>
    ResponsiblePeople.filter(people).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false))

  val max = (x: BigDecimal, y: BigDecimal) => if (x > y) x else y

}

object ResponsePeopleRowsInstances {

  implicit val responsePeopleRowsFromSubscription: ResponsePeopleRows[SubmissionResponse] = {
    new ResponsePeopleRows[SubmissionResponse] {
      def apply(value: SubmissionResponse, people: Seq[ResponsiblePeople], activities: Set[BusinessActivity]) = {
        if (showBreakdown(value.getFpFee, activities)) {

          splitPeopleByFitAndProperTest(people) match {
            case (passedFP, notFP) =>
              Seq(
                BreakdownRow(peopleRow(value).message, notFP.size, peopleRow(value).feePer, Currency.fromBD(value.getFpFee.getOrElse(0)))
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
    }
  }

}

object ResponsePeopleRows {
  def apply[A](
                value: A,
                people: Seq[ResponsiblePeople],
                activities: Set[BusinessActivity])(implicit r: ResponsePeopleRows[A]): Seq[BreakdownRow] = r(value, people, activities)
}