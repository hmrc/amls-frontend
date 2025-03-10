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

package views.tcsp

import forms.tcsp.OnlyOffTheShelfCompsSoldFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.OnlyOffTheShelfCompsSoldView

class OnlyOffTheShelfCompsSoldViewSpec extends AmlsViewSpec with Matchers {

  lazy val only_off_the_shelf_comps_sold = inject[OnlyOffTheShelfCompsSoldView]
  lazy val fp                            = inject[OnlyOffTheShelfCompsSoldFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "OnlyOffTheShelfCompsSoldView view" must {
    "have correct title, heading amd subheading" in new ViewFixture {

      def view = only_off_the_shelf_comps_sold(fp(), true)

      val title = messages("tcsp.off-the-shelf.companies.lbl") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      doc.title       must be(title)
      heading.html    must be(messages("tcsp.off-the-shelf.companies.lbl"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    behave like pageWithErrors(
      only_off_the_shelf_comps_sold(
        fp().withError("onlyOffTheShelfCompsSold", "error.required.tcsp.off.the.shelf.companies"),
        true
      ),
      "onlyOffTheShelfCompsSold",
      "error.required.tcsp.off.the.shelf.companies"
    )

    behave like pageWithBackLink(only_off_the_shelf_comps_sold(fp(), false))
  }
}
