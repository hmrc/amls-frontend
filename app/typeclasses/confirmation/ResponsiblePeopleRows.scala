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

import models.businessmatching.{BusinessActivity, TrustAndCompanyServices, MoneyServiceBusiness => MSB}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePerson
import models.{AmendVariationRenewalResponse, SubmissionResponse}

trait ResponsiblePeopleRows[A] extends FeeCalculations {
  def apply(
             value: A,
             activities: Set[BusinessActivity],
             people: Option[Seq[ResponsiblePerson]]
           ): Seq[BreakdownRow]
  
  val showBreakdown = (fpFee: Option[BigDecimal], activities: Set[BusinessActivity]) =>
    fpFee.fold(activities.exists(act => act == MSB || act == TrustAndCompanyServices)) { _ => true }

  val splitPeopleByFitAndProperTest = (people: Seq[ResponsiblePerson]) =>
    ResponsiblePerson.filter(people).partition(_.hasAlreadyPassedFitAndProper.getOrElse(false))

}

object ResponsiblePeopleRowsInstances {

  implicit val responsiblePeopleRowsFromSubscription: ResponsiblePeopleRows[SubmissionResponse] =
    new ResponsiblePeopleRows[SubmissionResponse] {
      def apply(value: SubmissionResponse, activities: Set[BusinessActivity], people: Option[Seq[ResponsiblePerson]]): Seq[BreakdownRow] = {

        people.fold(Seq.empty[BreakdownRow]) { responsiblePeople =>
          if (showBreakdown(value.getFpFee, activities)) {
            splitPeopleByFitAndProperTest(responsiblePeople) match {
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


  implicit val responsiblePeopleRowsFromVariation: ResponsiblePeopleRows[AmendVariationRenewalResponse] = {
    new ResponsiblePeopleRows[AmendVariationRenewalResponse] {
      override def apply(
                          value: AmendVariationRenewalResponse,
                          activities: Set[BusinessActivity],
                          people: Option[Seq[ResponsiblePerson]]): Seq[BreakdownRow] = {

        if (showBreakdown(value.getFpFee, activities)) {

          val (passedFP, notFP) = (value.addedResponsiblePeopleFitAndProper, value.addedResponsiblePeople)

          (if (notFP > 0) {
            Seq(BreakdownRow(
              peopleVariationRow(value).message,
              notFP,
              peopleVariationRow(value).feePer,
              Currency.fromBD(value.getFpFee.getOrElse(0))
            ))
          } else {
            Seq.empty
          }) ++ (if (passedFP > 0) {
            Seq(BreakdownRow(peopleFPPassed.message, passedFP, max(0, peopleFPPassed.feePer), Currency.fromBD(max(0, peopleFPPassed.feePer))))
          } else {
            Seq.empty
          })

        } else {
          Seq.empty
        }
      }
    }
  }
}

object ResponsiblePeopleRows {
  def apply[A](
                value: A,
                activities: Set[BusinessActivity],
                people: Option[Seq[ResponsiblePerson]])(implicit r: ResponsiblePeopleRows[A]): Seq[BreakdownRow] = r(value, activities, people)
}