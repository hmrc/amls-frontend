/*
 * Copyright 2024 HM Revenue & Customs
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

package utils.supervision

import models.supervision.{ProfessionalBodyYes, _}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryList, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import utils.{CheckYourAnswersHelperFunctions, DateHelper}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject() () extends CheckYourAnswersHelperFunctions {

  def getSummaryList(model: Supervision)(implicit messages: Messages): SummaryList =
    SummaryList(
      anotherBodyRows(model).getOrElse(Seq.empty[SummaryListRow]) ++ Seq(
        professionalBodyMemberRow(model),
        professionalBodiesRow(model)
      ).flatten ++ penalisedRows(model).getOrElse(Seq.empty[SummaryListRow])
    )

  private def anotherBodyRows(model: Supervision)(implicit messages: Messages): Option[Seq[SummaryListRow]] =
    model.anotherBody map {
      case AnotherBodyNo                                                                 =>
        Seq(
          row(
            "supervision.another_body.title",
            booleanToLabel(false),
            editAction(
              controllers.supervision.routes.AnotherBodyController.get(true).url,
              "supervision.checkYourAnswers.change.registeredBefore",
              "supervisionanotherbody-edit"
            )
          )
        )
      case AnotherBodyYes(supervisorName, Some(startDate), Some(endDate), Some(reasons)) =>
        Seq(
          row(
            "supervision.another_body.title",
            booleanToLabel(true),
            editAction(
              controllers.supervision.routes.AnotherBodyController.get(true).url,
              "supervision.checkYourAnswers.change.registeredBefore",
              "supervisionanotherbody-edit-name"
            )
          ),
          row(
            "supervision.another_body.lbl.supervisor",
            supervisorName,
            editAction(
              controllers.supervision.routes.AnotherBodyController.get(true).url,
              "supervision.checkYourAnswers.change.prevSupervisor",
              "supervisionanotherbody-edit-previous-name"
            )
          ),
          row(
            "supervision.supervision_start.title",
            DateHelper.formatDate(startDate.startDate),
            editAction(
              controllers.supervision.routes.SupervisionStartController.get(true).url,
              "supervision.checkYourAnswers.change.whenLastStrt",
              "supervisionanotherbody-edit-start-date"
            )
          ),
          row(
            "supervision.supervision_end.title",
            DateHelper.formatDate(endDate.endDate),
            editAction(
              controllers.supervision.routes.SupervisionEndController.get(true).url,
              "supervision.checkYourAnswers.change.whenLastEnd",
              "supervisionanotherbody-edit-end-date"
            )
          ),
          row(
            "supervision.supervision_end_reasons.title",
            reasons.endingReason,
            editAction(
              controllers.supervision.routes.SupervisionEndReasonsController.get(true).url,
              "supervision.checkYourAnswers.change.whyLastEnd",
              "supervisionanotherbody-edit-ending-reason"
            )
          )
        )
    }

  private def professionalBodyMemberRow(model: Supervision)(implicit messages: Messages): Option[SummaryListRow] = {
    val answerOpt = model.professionalBodyMember.map {
      case ProfessionalBodyMemberYes => booleanToLabel(true)
      case ProfessionalBodyMemberNo  => booleanToLabel(false)
    }

    answerOpt map { answer =>
      row(
        "supervision.memberofprofessionalbody.title",
        answer,
        editAction(
          controllers.supervision.routes.ProfessionalBodyMemberController.get(true).url,
          "supervision.checkYourAnswers.change.professionalBodiesMmbr",
          "supervisionmemberofbody-edit"
        )
      )
    }
  }

  private def professionalBodiesRow(model: Supervision)(implicit messages: Messages): Option[SummaryListRow] = {
    val answerOpt = model.professionalBodies.map {
      case ProfessionalBodies(businessTypes) if businessTypes.size == 1 =>
        Value(Text(businessTypes.map(_.getMessage()).mkString))
      case ProfessionalBodies(businessTypes) if businessTypes.size > 1  =>
        toBulletList(businessTypes.map(_.getMessage()).toSeq.sorted)
    }

    answerOpt map { answer =>
      SummaryListRow(
        Key(Text(messages("supervision.whichprofessionalbody.title"))),
        answer,
        actions = editAction(
          controllers.supervision.routes.WhichProfessionalBodyController.get(true).url,
          "supervision.checkYourAnswers.change.whichProfessionalBodiesMmbr",
          "supervisionwhichbody-edit"
        )
      )
    }
  }

  private def penalisedRows(model: Supervision)(implicit messages: Messages): Option[Seq[SummaryListRow]] =
    model.professionalBody.map {
      case ProfessionalBodyNo           =>
        Seq(
          row(
            "supervision.penalisedbyprofessional.title",
            booleanToLabel(false),
            editAction(
              controllers.supervision.routes.PenalisedByProfessionalController.get(true).url,
              "supervision.checkYourAnswers.change.penaltyDtls",
              "penalisedbyprofessional-edit"
            )
          )
        )
      case ProfessionalBodyYes(details) =>
        Seq(
          row(
            "supervision.penalisedbyprofessional.heading1",
            booleanToLabel(true),
            editAction(
              controllers.supervision.routes.PenalisedByProfessionalController.get(true).url,
              "supervision.checkYourAnswers.change.penaltyDtls",
              "penalisedbyprofessional-edit"
            )
          ),
          row(
            "supervision.penaltydetails.title",
            details,
            editAction(
              controllers.supervision.routes.PenaltyDetailsController.get(true).url,
              "supervision.checkYourAnswers.change.penaltyDtlsDesc",
              "penaltydetails-edit"
            )
          )
        )
    }
}
