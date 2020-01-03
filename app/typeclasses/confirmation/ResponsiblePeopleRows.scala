/*
 * Copyright 2020 HM Revenue & Customs
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

import models.businessmatching.BusinessActivity
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.ResponsiblePerson
import models.{AmendVariationRenewalResponse, SubmissionResponse}
import play.api.Logger
import utils.{ActivitiesHelper, StatusConstants}

trait ResponsiblePeopleRows[A] extends FeeCalculations {
  def apply(
             value: A,
             activities: Set[BusinessActivity],
             people: Option[Seq[ResponsiblePerson]]
           ): Seq[BreakdownRow]

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

    Logger.debug(s"[createBreakdownRowForAmendVariationRenewalResponse] - value: ${value}")

    val notPassedFP = value.addedResponsiblePeopleFitAndProper
    val notPassedApprovalCheck = value.addedResponsiblePeopleApprovalCheck

    if (ActivitiesHelper.fpSectors(activities) && (notPassedFP > 0) ) {
      Seq(
        BreakdownRow(
          peopleRow(value).message,
          value.addedResponsiblePeopleFitAndProper,
          peopleRow(value).feePer,
          Currency.fromBD(value.getFpFee.getOrElse(0))
        ))
    } else if (ActivitiesHelper.acSectors(activities) && (notPassedApprovalCheck > 0)) {
      Seq(
        BreakdownRow(
          approvalCheckPeopleRow(value).message,
          value.addedResponsiblePeopleApprovalCheck,
          approvalCheckPeopleRow(value).feePer,
          Currency.fromBD(value.getApprovalCheckFee.getOrElse(0))
        ))
    } else {
      Seq.empty
    }
  }

  def createBreakdownRowForSubmissionResponse(
                                               value: SubmissionResponse,
                                               people: Option[Seq[ResponsiblePerson]],
                                               activities: Set[BusinessActivity]
                                             ) = {

    Logger.debug(s"[createBreakdownRowForSubmissionResponse] - value: ${value}")

    val fitAndProperCount = countNonDeletedPeopleWhoHaventPassedFitAndProper(people.getOrElse(Seq.empty))
    val approvalCheckCount = countNonDeletedPeopleWhoHaventPassedApprovalCheck(people.getOrElse(Seq.empty))

    if (ActivitiesHelper.fpSectors(activities) && (fitAndProperCount > 0) && (hasFpFee(value))) {
      Seq(
        BreakdownRow(
          peopleRow(value).message,
          fitAndProperCount,
          peopleRow(value).feePer,
          Currency.fromBD(value.getFpFee.getOrElse(0))
        ))
    } else if (ActivitiesHelper.acSectors(activities) && (approvalCheckCount > 0) && (hasApprovalFee(value))) {
      Seq(
        BreakdownRow(
          approvalCheckPeopleRow(value).message,
          approvalCheckCount,
          approvalCheckPeopleRow(value).feePer,
          Currency.fromBD(value.getApprovalCheckFee.getOrElse(0))
        ))
    } else {
      Seq.empty
    }
  }

  def hasApprovalFee(value: SubmissionResponse): Boolean = {
    value.getApprovalCheckFee.nonEmpty && value.getApprovalCheckFee.get > 0
  }

  def hasFpFee(value: SubmissionResponse): Boolean = {
    value.getFpFee.nonEmpty && value.getFpFee.get > 0
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

object ResponsiblePeopleRows {
  def apply[A](
                value: A,
                activities: Set[BusinessActivity],
                people: Option[Seq[ResponsiblePerson]])(implicit r: ResponsiblePeopleRows[A]): Seq[BreakdownRow] = r(value, activities, people)
}