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

package views.tradingpremises

import forms.tradingpremises.ActivityStartDateFormProvider
import models.tradingpremises.Address
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Request
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.ActivityStartDateView

class ActivityStartDateViewSpec extends AmlsViewSpec with Matchers {

  lazy val activity_start_date = inject[ActivityStartDateView]
  lazy val fp                  = inject[ActivityStartDateFormProvider]

  implicit val request: Request[_] = FakeRequest()

  val addrLine1 = "Line 1"
  val addrLine2 = "Line 2"
  val postcode  = "PO5 1CD"
  val address   = Address(addrLine1, Some(addrLine2), None, None, postcode)

  "ActivityStartDateView" must {
    "have correct title, heading and load UI with empty form" in new Fixture {

      val pageTitle = messages("tradingpremises.startDate.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = activity_start_date(fp(), 1, false, address)

      doc.html        must (include(addrLine1) and include(addrLine2) and include(postcode))
      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.startDate.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.getElementsContainingOwnText(messages("lbl.day")).hasText   must be(true)
      doc.getElementsContainingOwnText(messages("lbl.month")).hasText must be(true)
      doc.getElementsContainingOwnText(messages("lbl.year")).hasText  must be(true)

      doc.getElementsContainingOwnText(messages("lbl.date.example")).hasText must be(true)
    }

    behave like pageWithErrors(
      activity_start_date(
        fp().withError("startDate.day", messages("error.required.tp.address.date.one", messages("lbl.day"))),
        1,
        false,
        address
      ),
      "startDate",
      messages("error.required.tp.address.date.one", messages("lbl.day"))
    )

    behave like pageWithBackLink(activity_start_date(fp(), 1, true, address))
  }
}
