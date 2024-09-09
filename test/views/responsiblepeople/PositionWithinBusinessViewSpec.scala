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

import forms.responsiblepeople.PositionWithinBusinessFormProvider
import models.businessmatching.BusinessType
import models.responsiblepeople.{BeneficialOwner, Director, InternalAccountant, NominatedOfficer, PositionWithinBusiness}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PositionWithinBusinessView

class PositionWithinBusinessViewSpec extends AmlsViewSpec with Matchers {

  lazy val businessView: PositionWithinBusinessView = inject[PositionWithinBusinessView]
  lazy val fp: PositionWithinBusinessFormProvider = inject[PositionWithinBusinessFormProvider]

  val name = "firstName lastName"

  val positions: Seq[PositionWithinBusiness] = Seq(
    BeneficialOwner,
    Director,
    InternalAccountant,
    NominatedOfficer
  )

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PositionWithinBusinessView" must {

    "have correct title" in new ViewFixture {

      def view: HtmlFormat.Appendable = businessView(fp(), edit = true, 1, BusinessType.SoleProprietor, name, displayNominatedOfficer = true, None, positions)
      doc.title must be(messages("responsiblepeople.position_within_business.title") +
        " - " + messages("summary.responsiblepeople") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {
      def view: HtmlFormat.Appendable = businessView(fp(), edit = true, 1, BusinessType.SoleProprietor, name, displayNominatedOfficer = true, None, positions)
      heading.html must be(messages("responsiblepeople.position_within_business.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    behave like pageWithErrors(
      businessView(
        fp().withError("positions", "error.required.positionWithinBusiness"),
        edit = true,
        1,
        BusinessType.SoleProprietor,
        name,
        displayNominatedOfficer = true,
        None,
        positions
      ),
      "positions",
      "error.required.positionWithinBusiness"
    )

    behave like pageWithErrors(
      businessView(
        fp().withError("otherPosition", "error.invalid.rp.position_within_business.other_position"),
        edit = true,
        1,
        BusinessType.SoleProprietor,
        name,
        displayNominatedOfficer = true,
        None,
        positions
      ),
      "otherPosition",
      "error.invalid.rp.position_within_business.other_position"
    )

    behave like pageWithBackLink(businessView(fp(), edit = true, 1, BusinessType.SoleProprietor, name, displayNominatedOfficer = true, None, positions))
  }
}
