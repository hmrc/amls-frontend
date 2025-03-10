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

import forms.responsiblepeople.address.AdditionalAddressFormProvider
import models.responsiblepeople.{PersonAddressUK, ResponsiblePersonAddress}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.AdditionalExtraAddressUKView

class AdditionalExtraAddressUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val uKView = inject[AdditionalExtraAddressUKView]
  lazy val fp     = inject[AdditionalAddressFormProvider]

  val name = "firstName lastName"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AdditionalExtraAddressUKView" must {

    "have correct title" in new ViewFixture {

      def view = uKView(
        fp().fill(ResponsiblePersonAddress(PersonAddressUK("", None, None, None, ""), None)),
        true,
        1,
        None,
        name
      )

      doc.title must startWith(messages("responsiblepeople.additional_extra_address_country.title", name))
    }

    "have correct headings" in new ViewFixture {

      def view = uKView(
        fp().fill(ResponsiblePersonAddress(PersonAddressUK("", None, None, None, ""), None)),
        true,
        1,
        None,
        name
      )

      heading.html    must be(messages("responsiblepeople.additional_extra_address_country.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

    }

    Seq(1, 2, 3, 4) foreach { line =>
      behave like pageWithErrors(
        uKView(
          fp().withError(s"addressLine$line", s"error.text.validation.address.line$line"),
          false,
          1,
          None,
          name
        ),
        s"addressLine$line",
        s"error.text.validation.address.line$line"
      )
    }

    behave like pageWithErrors(
      uKView(
        fp().withError("postCode", "error.required.postcode"),
        false,
        1,
        None,
        name
      ),
      "postCode",
      "error.required.postcode"
    )

    behave like pageWithBackLink(uKView(fp(), false, 1, None, name))
  }
}
