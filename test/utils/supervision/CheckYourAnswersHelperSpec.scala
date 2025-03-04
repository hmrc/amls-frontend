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

import models.supervision.ProfessionalBodies.{AccountingTechnicians, Other}
import models.supervision._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{AmlsSpec, DateHelper}

import java.time.LocalDate

class CheckYourAnswersHelperSpec extends AmlsSpec {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val otherService = "Carrier pigeon"

  val anotherBody = AnotherBodyYes(
    "Supervison Name",
    Some(SupervisionStart(LocalDate.now().minusYears(5))),
    Some(SupervisionEnd(LocalDate.now().minusYears(1))),
    Some(SupervisionEndReasons("This is why I ended supervision"))
  )

  val businessTypes = ProfessionalBodies.all.map { x =>
    if (x.value == Other("").value) {
      Other("Business details")
    } else {
      x
    }
  }

  val professionalBody = ProfessionalBodyYes("This is the body that penalised me")

  val model: Supervision = Supervision(
    Some(anotherBody),
    Some(ProfessionalBodyMemberYes),
    Some(ProfessionalBodies(businessTypes.toSet)),
    Some(professionalBody)
  )

  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }

    def toBulletList[A](coll: Seq[A]): String =
      "<ul class=\"govuk-list govuk-list--bullet\">" +
        coll.map { x =>
          s"<li>$x</li>"
        }.mkString +
        "</ul>"

    def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
      messages("lbl.yes")
    } else {
      messages("lbl.no")
    }
  }

  ".createSummaryList" when {

    "Another body is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(anotherBody = Some(AnotherBodyNo))
          )
          .rows

        assertRowMatches(
          0,
          "supervision.another_body.title",
          booleanToLabel(false),
          controllers.supervision.routes.AnotherBodyController.get(true).url,
          "supervisionanotherbody-edit"
        )
      }

      "render the correct content for 'Yes'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          0,
          "supervision.another_body.title",
          booleanToLabel(true),
          controllers.supervision.routes.AnotherBodyController.get(true).url,
          "supervisionanotherbody-edit-name"
        )
      }

      "render the correct content for Supervisor name" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          1,
          "supervision.another_body.lbl.supervisor",
          anotherBody.supervisorName,
          controllers.supervision.routes.AnotherBodyController.get(true).url,
          "supervisionanotherbody-edit-previous-name"
        )
      }

      "render the correct content for Start Date" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          2,
          "supervision.supervision_start.title",
          DateHelper.formatDate(anotherBody.startDate.value.startDate),
          controllers.supervision.routes.SupervisionStartController.get(true).url,
          "supervisionanotherbody-edit-start-date"
        )
      }

      "render the correct content for End Date" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          3,
          "supervision.supervision_end.title",
          DateHelper.formatDate(anotherBody.endDate.value.endDate),
          controllers.supervision.routes.SupervisionEndController.get(true).url,
          "supervisionanotherbody-edit-end-date"
        )
      }

      "render the correct content for End reasons" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          4,
          "supervision.supervision_end_reasons.title",
          anotherBody.endingReason.value.endingReason,
          controllers.supervision.routes.SupervisionEndReasonsController.get(true).url,
          "supervisionanotherbody-edit-ending-reason"
        )
      }
    }

    "Professional body member is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(professionalBodyMember = Some(ProfessionalBodyMemberNo))
          )
          .rows

        assertRowMatches(
          5,
          "supervision.memberofprofessionalbody.title",
          booleanToLabel(false),
          controllers.supervision.routes.ProfessionalBodyMemberController.get(true).url,
          "supervisionmemberofbody-edit"
        )
      }

      "render the correct content for 'Yes'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          5,
          "supervision.memberofprofessionalbody.title",
          booleanToLabel(true),
          controllers.supervision.routes.ProfessionalBodyMemberController.get(true).url,
          "supervisionmemberofbody-edit"
        )
      }
    }

    "Professional Bodies is present" must {

      "render the correct content for a single type" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(professionalBodies = Some(ProfessionalBodies(Set(AccountingTechnicians))))
          )
          .rows

        assertRowMatches(
          6,
          "supervision.whichprofessionalbody.title",
          AccountingTechnicians.getMessage(),
          controllers.supervision.routes.WhichProfessionalBodyController.get(true).url,
          "supervisionwhichbody-edit"
        )
      }

      "render the correct content for multiple types" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          6,
          "supervision.whichprofessionalbody.title",
          toBulletList(businessTypes.map(_.getMessage()).sorted),
          controllers.supervision.routes.WhichProfessionalBodyController.get(true).url,
          "supervisionwhichbody-edit"
        )
      }
    }

    "Professional Body is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(professionalBody = Some(ProfessionalBodyNo))
          )
          .rows

        assertRowMatches(
          7,
          "supervision.penalisedbyprofessional.title",
          booleanToLabel(false),
          controllers.supervision.routes.PenalisedByProfessionalController.get(true).url,
          "penalisedbyprofessional-edit"
        )
      }

      "render the correct content for 'Yes'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          7,
          "supervision.penalisedbyprofessional.heading1",
          booleanToLabel(true),
          controllers.supervision.routes.PenalisedByProfessionalController.get(true).url,
          "penalisedbyprofessional-edit"
        )
      }

      "render the correct content for reason" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          8,
          "supervision.penaltydetails.title",
          professionalBody.value,
          controllers.supervision.routes.PenaltyDetailsController.get(true).url,
          "penaltydetails-edit"
        )
      }
    }
  }
}
