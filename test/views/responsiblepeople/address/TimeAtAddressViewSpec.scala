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

package views.responsiblepeople.address

import forms.responsiblepeople.address.TimeAtAddressFormProvider
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.TimeAtAddressView

class TimeAtAddressViewSpec extends AmlsViewSpec with Matchers  {

  lazy val time_at_address = inject[TimeAtAddressView]
  lazy val fp = inject[TimeAtAddressFormProvider]

  val name = "FirstName LastName"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "TimeAtAddressView" must {

    "have correct title" in new ViewFixture {

      def view = time_at_address(fp().fill(ZeroToFiveMonths), false, 0, None, name)

      doc.title must be(messages("responsiblepeople.timeataddress.address_history.current.title") +
        " - " + messages("summary.responsiblepeople") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct heading" in new ViewFixture {

      def view = time_at_address(fp().fill(ZeroToFiveMonths), false, 0, None, name)

      heading.html() must be(messages("responsiblepeople.timeataddress.address_history.current.heading", name))
    }

    behave like pageWithErrors(
      time_at_address(fp().withError("timeAtAddress", "error.required.timeAtAddress"), false, 1 , None, name),
      "timeAtAddress",
      "error.required.timeAtAddress"
    )

    behave like pageWithBackLink(time_at_address(fp(), false, 1, None, name))
  }


}
