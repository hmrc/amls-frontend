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

package views.responsiblepeople

import forms.responsiblepeople.PositionWithinBusinessStartDateFormProvider
import models.businessmatching.BusinessType
import models.responsiblepeople._
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PositionWithinBusinessStartDateView

class PositionWithinBusinessStartDateViewSpec extends AmlsViewSpec with Matchers {

  lazy val dateView = inject[PositionWithinBusinessStartDateView]
  lazy val fp       = inject[PositionWithinBusinessStartDateFormProvider]

  val name = "firstName lastName"

  val positions: Set[PositionWithinBusiness] = Set(Director, NominatedOfficer, Partner, Other("Wizard"))

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PositionWithinBusinessStartDateView view" must {

    "have correct title" in new ViewFixture {
      def view = dateView(fp(), true, 1, BusinessType.SoleProprietor, name, Set(), true, None)
      doc.title must be(
        messages("responsiblepeople.position_within_business.startDate.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {
      def view = dateView(fp(), true, 1, BusinessType.SoleProprietor, name, Set(), true, None)
      heading.html    must be(messages("responsiblepeople.position_within_business.startDate.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "display inline text for a single position" in new ViewFixture {
      val positions = Set(NominatedOfficer).asInstanceOf[Set[PositionWithinBusiness]]
      def view      = dateView(fp(), true, 1, BusinessType.SoleProprietor, name, positions, true, None)
      doc
        .text()
        .contains(
          messages(
            "responsiblepeople.position_within_business.startDate.toldus.single",
            name,
            PositionWithinBusiness.getPrettyName(NominatedOfficer).toLowerCase
          )
        ) mustBe true
      doc.select("li.business-role").isEmpty mustBe true
    }

    "display bullet list for multiple positions" in new ViewFixture {
      def view = dateView(fp(), true, 1, BusinessType.SoleProprietor, name, positions, true, None)
      doc
        .text()
        .contains(messages("responsiblepeople.position_within_business.startDate.toldus.multiple", name)) mustBe true
      doc
        .text()
        .contains(messages("responsiblepeople.position_within_business.startDate.toldus.selectfirst")) mustBe true
      doc.select("li.business-role").size() mustBe positions.size
    }

    behave like pageWithErrors(
      dateView(
        fp().withError("startDate", "error.rp.position.required.date.all"),
        false,
        1,
        BusinessType.SoleProprietor,
        name,
        positions,
        true,
        None
      ),
      "startDate",
      "error.rp.position.required.date.all"
    )

    behave like pageWithBackLink(dateView(fp(), true, 1, BusinessType.SoleProprietor, name, positions, true, None))
  }
}
