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

import forms.responsiblepeople.address.MovedAddressFormProvider
import models.responsiblepeople.PersonAddressUK
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.MovedAddressView

class MovedAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val moved_address = inject[MovedAddressView]
  lazy val fp            = inject[MovedAddressFormProvider]

  val name    = "firstName lastName"
  val address = PersonAddressUK("#11", Some("some building"), Some("Some street"), Some("city"), "AA111AA")

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "MovedAddressView view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = moved_address(fp(), address, 1, name)

      doc.title       must be(
        messages("responsiblepeople.movedaddress.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("responsiblepeople.movedaddress.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "movedAddress") must not be empty
    }

    behave like pageWithErrors(
      moved_address(fp().withError("movedAddress", "error.required.rp.moved.address"), address, 1, name),
      "movedAddress",
      "error.required.rp.moved.address"
    )

    behave like pageWithBackLink(moved_address(fp(), address, 1, name))
  }
}
