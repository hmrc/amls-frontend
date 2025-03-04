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

import forms.responsiblepeople.address.AdditionalExtraAddressFormProvider
import models.responsiblepeople.{PersonAddressUK, ResponsiblePersonAddress}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.AdditionalExtraAddressView

class AdditionalExtraAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val extraAddressView = inject[AdditionalExtraAddressView]
  lazy val fp               = inject[AdditionalExtraAddressFormProvider]

  val name = "firstName lastName"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AdditionalExtraAddressView view" must {

    "have correct title" in new ViewFixture {

      def view = extraAddressView(
        fp().fill(ResponsiblePersonAddress(PersonAddressUK("", None, None, None, ""), None)),
        true,
        1,
        None,
        name
      )

      doc.title must startWith(messages("responsiblepeople.additional_extra_address.title", name))
    }

    "have correct headings" in new ViewFixture {

      def view = extraAddressView(
        fp().fill(ResponsiblePersonAddress(PersonAddressUK("", None, None, None, ""), None)),
        true,
        1,
        None,
        name
      )

      heading.html    must be(messages("responsiblepeople.additional_extra_address.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    behave like pageWithErrors(
      extraAddressView(
        fp().withError("isUK", "error.required.uk.or.overseas.address.previous.other"),
        false,
        1,
        None,
        name
      ),
      "isUK",
      "error.required.uk.or.overseas.address.previous.other"
    )

    pageWithBackLink(extraAddressView(fp(), false, 1, None, name))
  }
}
