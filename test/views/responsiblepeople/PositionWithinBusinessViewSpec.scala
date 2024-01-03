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
import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PositionWithinBusinessView

class PositionWithinBusinessViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val businessView = inject[PositionWithinBusinessView]
  lazy val fp = inject[PositionWithinBusinessFormProvider]

  val name = "firstName lastName"

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "PositionWithinBusinessView" must {

    "have correct title" in new ViewFixture {

      def view = businessView(fp(), true, 1, BusinessType.SoleProprietor, name, true, None)
      doc.title must be(messages("responsiblepeople.position_within_business.title") +
        " - " + messages("summary.responsiblepeople") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {
      val form2 = EmptyForm
      def view = businessView(fp(), true, 1, BusinessType.SoleProprietor, name, true, None)
      heading.html must be(messages("responsiblepeople.position_within_business.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    behave like pageWithErrors(
      businessView(
        fp().withError("positions", "error.required.positionWithinBusiness"),
        true,
        1,
        BusinessType.SoleProprietor,
        name,
        true,
        None
      ),
      "positions",
      "error.required.positionWithinBusiness"
    )

    behave like pageWithErrors(
      businessView(
        fp().withError("otherPosition", "error.invalid.rp.position_within_business.other_position"),
        true,
        1,
        BusinessType.SoleProprietor,
        name,
        true,
        None
      ),
      "otherPosition",
      "error.invalid.rp.position_within_business.other_position"
    )

    behave like pageWithBackLink(businessView(fp(), true, 1, BusinessType.SoleProprietor, name, true, None))
  }
}
