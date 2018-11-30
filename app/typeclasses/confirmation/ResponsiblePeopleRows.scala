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
import models.businessmatching.{BusinessActivity, TrustAndCompanyServices, MoneyServiceBusiness => MSB,
  BillPaymentServices => BPS, TelephonePaymentService => TPS}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePerson
import models.{AmendVariationRenewalResponse, SubmissionResponse}
import utils.StatusConstants

trait ResponsiblePeopleRows[A] extends FeeCalculations {
  def apply(
             value: A,
             activities: Set[BusinessActivity],
             people: Option[Seq[ResponsiblePerson]]
           ): Seq[BreakdownRow]

  val showBreakdown = (fpFee: Option[BigDecimal], activities: Set[BusinessActivity]) =>
    if (ApplicationConfig.phase2ChangesToggle) {
      true
    } else {
      fpFee.fold(activities.exists(act => act == MSB || act == TrustAndCompanyServices)) { _ => true }
    }

  val splitPeopleByFitAndProperTest = (people: Seq[ResponsiblePerson]) =>
    ResponsiblePerson.filter(people).partition(_.approvalFlags.hasAlreadyPassedFitAndProper.getOrElse(false))

  def countNonDeletedPeopleWhoHaventPassedApprovalCheck(people: Seq[ResponsiblePerson]) =
    people.count(x => x.approvalFlags.hasAlreadyPaidApprovalCheck.contains(false) && !x.status.contains(StatusConstants.Deleted))

  def countNonDeletedPeopleWhoHaventPassedFitAndProper(people: Seq[ResponsiblePerson]): Int =
    people.count(x => x.approvalFlags.hasAlreadyPassedFitAndProper.contains(false) && !x.status.contains(StatusConstants.Deleted))

  def createBreakdownRowForAmendVariationRenewalResponse(value: AmendVariationRenewalResponse,
                                                         people: Option[Seq[ResponsiblePerson]],
                                                         activities: Set[BusinessActivity]
                                                        ): Seq[BreakdownRow] = {

    val (notPassedFP, notPassedApprovalCheck) = (value.addedResponsiblePeopleFitAndProper, value.addedResponsiblePeopleApprovalCheck)
    val msbOrTcsp = (activities.contains(MSB) || activities.contains(TrustAndCompanyServices))
    val bpsTdi = (activities.contains(BPS) || activities.contains(TPS))

    (notPassedFP > 0, notPassedApprovalCheck > 0, msbOrTcsp, bpsTdi) match {

      case (_, true, false, false) => Seq(
        BreakdownRow(
          approvalCheckPeopleRow(value).message,
          value.addedResponsiblePeopleApprovalCheck,
          approvalCheckPeopleRow(value).feePer,
          Currency.fromBD(value.getApprovalCheckFee.getOrElse(0))
        ))

      case (true, _, _, false) => Seq(
        BreakdownRow(
          peopleRow(value).message,
          value.addedResponsiblePeopleFitAndProper,
          peopleRow(value).feePer,
          Currency.fromBD(value.getFpFee.getOrElse(0))
        ))

      case (_, _, _, _) => Seq.empty
    }
  }

  def createBreakdownRowForSubmissionResponse(
                                               value: SubmissionResponse,
                                               people: Option[Seq[ResponsiblePerson]],
                                               activities: Set[BusinessActivity]
                                             ) = {
    val fitAndProperCount = countNonDeletedPeopleWhoHaventPassedFitAndProper(people.getOrElse(Seq.empty))
    val approvalCheckCount = countNonDeletedPeopleWhoHaventPassedApprovalCheck(people.getOrElse(Seq.empty))
    val msbOrTcsp = (activities.contains(MSB) || activities.contains(TrustAndCompanyServices))
    val bpsTdi = (activities.contains(BPS) || activities.contains(TPS))

    (fitAndProperCount > 0, approvalCheckCount > 0, msbOrTcsp, bpsTdi) match {

      case (_, true, false, false) =>  Seq(
        BreakdownRow(
          approvalCheckPeopleRow(value).message,
          approvalCheckCount,
          approvalCheckPeopleRow(value).feePer,
          Currency.fromBD(value.getApprovalCheckFee.getOrElse(0))
        ))

      case (true, _, true, false) =>  Seq(
        BreakdownRow(
          peopleRow(value).message,
          fitAndProperCount,
          peopleRow(value).feePer,
          Currency.fromBD(value.getFpFee.getOrElse(0))
        ))

      case (_, _, _, _) => Seq.empty
    }
  }
}

object ResponsiblePeopleRowsInstancesPhase2 {

  implicit val responsiblePeopleRowsFromSubscription: ResponsiblePeopleRows[SubmissionResponse] =
    new ResponsiblePeopleRows[SubmissionResponse] {
      def apply(
                 value: SubmissionResponse,
                 activities: Set[BusinessActivity],
                 people: Option[Seq[ResponsiblePerson]]
               ): Seq[BreakdownRow] = {

        createBreakdownRowForSubmissionResponse(value, people, activities)
      }
    }

  implicit val responsiblePeopleRowsFromVariation: ResponsiblePeopleRows[AmendVariationRenewalResponse] = {
    new ResponsiblePeopleRows[AmendVariationRenewalResponse] {
      def apply(
                 value: AmendVariationRenewalResponse,
                 activities: Set[BusinessActivity],
                 people: Option[Seq[ResponsiblePerson]]): Seq[BreakdownRow] = {

        createBreakdownRowForAmendVariationRenewalResponse(value, people, activities)
      }
    }
  }
}

object ResponsiblePeopleRowsInstances {

  implicit val responsiblePeopleRowsFromSubscription: ResponsiblePeopleRows[SubmissionResponse] =
    new ResponsiblePeopleRows[SubmissionResponse] {
      def apply(
                 value: SubmissionResponse,
                 activities: Set[BusinessActivity],
                 people: Option[Seq[ResponsiblePerson]]
               ): Seq[BreakdownRow] = {

        if (!ApplicationConfig.phase2ChangesToggle) {

          val firstSeq = people.fold(Seq.empty[BreakdownRow]) { responsiblePeople =>
            if (showBreakdown(value.getFpFee, activities)) {
              splitPeopleByFitAndProperTest(responsiblePeople) match {
                case (_, notFP) if notFP.nonEmpty =>
                  Seq(
                    BreakdownRow(
                      peopleRow(value).message,
                      notFP.size,
                      peopleRow(value).feePer,
                      Currency.fromBD(value.getFpFee.getOrElse(0))
                    ))

                case (_, _) => Seq.empty
              }
            } else {
              Seq.empty
            }
          }
          firstSeq
        } else {
          createBreakdownRowForSubmissionResponse(value, people, activities)
        }
      }
    }

  implicit val responsiblePeopleRowsFromVariation: ResponsiblePeopleRows[AmendVariationRenewalResponse] = {
    new ResponsiblePeopleRows[AmendVariationRenewalResponse] {
      override def apply(
                          value: AmendVariationRenewalResponse,
                          activities: Set[BusinessActivity],
                          people: Option[Seq[ResponsiblePerson]]): Seq[BreakdownRow] = {

        if (!ApplicationConfig.phase2ChangesToggle) {

          val firstSeq = if (showBreakdown(value.getFpFee, activities)) {

            val notFP = value.addedResponsiblePeople

            if (notFP > 0) {
              Seq(BreakdownRow(
                peopleVariationRow(value).message,
                notFP,
                peopleVariationRow(value).feePer,
                Currency.fromBD(value.getFpFee.getOrElse(0))
              ))
            } else {
              Seq.empty
            }
          } else {
            Seq.empty
          }
          firstSeq
        } else {
          createBreakdownRowForAmendVariationRenewalResponse(value, people, activities)
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