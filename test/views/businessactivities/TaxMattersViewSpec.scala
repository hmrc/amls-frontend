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

package views.businessactivities

import forms.businessactivities.TaxMattersFormProvider
import models.businessactivities.TaxMatters
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.TaxMattersView

class TaxMattersViewSpec extends AmlsViewSpec with Matchers {

  lazy val matters = inject[TaxMattersView]
  lazy val fp      = inject[TaxMattersFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val accountantName = "Accountant name"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "TaxMattersView view" must {
    "have correct title" in new ViewFixture {

      def view = matters(fp().fill(TaxMatters(true)), true, accountantName)

      doc.title must startWith(messages("businessactivities.tax.matters.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = matters(fp().fill(TaxMatters(false)), true, accountantName)

      heading.html    must be(messages("businessactivities.tax.matters.heading", accountantName))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      matters(fp().withError("manageYourTaxAffairs", "error.required.ba.tax.matters"), true, accountantName),
      "manageYourTaxAffairs",
      "error.required.ba.tax.matters"
    )

    behave like pageWithBackLink(matters(fp(), true, accountantName))
  }
}
