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

package views.asp

import forms.asp.OtherBusinessTaxMattersFormProvider
import models.asp.{OtherBusinessTaxMattersNo, OtherBusinessTaxMattersYes}
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.asp.OtherBusinessTaxMattersView

class OtherBusinessTaxMattersViewSpec extends AmlsViewSpec with Matchers  {

  lazy val taxMatters = inject[OtherBusinessTaxMattersView]
  lazy val fp = inject[OtherBusinessTaxMattersFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "OtherBusinessTaxMattersView" must {

    "have correct title" in new ViewFixture {

      def view = taxMatters(fp().fill(OtherBusinessTaxMattersYes), true)

      doc.title must startWith(messages("asp.other.business.tax.matters.title") + " - " + messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {

      def view = taxMatters(fp().fill(OtherBusinessTaxMattersNo), true)

      heading.html must be(messages("asp.other.business.tax.matters.title"))
      subHeading.html must include(messages("summary.asp"))

    }

    behave like pageWithErrors(
      taxMatters(fp().withError("otherBusinessTaxMatters", "error.required.asp.other.business.tax.matters"), true),
      "otherBusinessTaxMatters",
      "error.required.asp.other.business.tax.matters"
    )

    behave like pageWithBackLink(taxMatters(fp(), false))
  }
}
