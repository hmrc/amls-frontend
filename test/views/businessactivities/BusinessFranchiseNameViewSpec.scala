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

import forms.businessactivities.BusinessFranchiseFormProvider
import models.businessactivities.{BusinessFranchiseNo, BusinessFranchiseYes}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import play.api.test.FakeRequest
import views.Fixture
import views.html.businessactivities.BusinessFranchiseNameView

class BusinessFranchiseNameViewSpec extends AmlsViewSpec with Matchers {

  lazy val franchise    = inject[BusinessFranchiseNameView]
  lazy val formProvider = inject[BusinessFranchiseFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "business_franchise_name view" must {
    "have correct title" in new ViewFixture {

      def view = franchise(formProvider().fill(BusinessFranchiseYes("Franchise name")), true)

      doc.title must startWith(
        messages("businessactivities.businessfranchise.title") + " - " + messages("summary.businessactivities")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = franchise(formProvider().fill(BusinessFranchiseNo), true)

      heading.html    must be(messages("businessactivities.businessfranchise.title"))
      subHeading.html must include(messages("summary.businessactivities"))
    }

    behave like pageWithErrors(
      franchise(formProvider().withError("businessFranchise", "error.required.ba.is.your.franchise"), false),
      "businessFranchise",
      "error.required.ba.is.your.franchise"
    )

    behave like pageWithErrors(
      franchise(formProvider().withError("franchiseName", "error.required.ba.franchise.name"), true),
      "franchiseName",
      "error.required.ba.franchise.name"
    )

    behave like pageWithBackLink(franchise(formProvider(), true))
  }
}
