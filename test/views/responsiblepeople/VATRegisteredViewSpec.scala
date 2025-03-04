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

import forms.responsiblepeople.VATRegisteredFormProvider
import models.responsiblepeople.{VATRegisteredNo, VATRegisteredYes}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.VATRegisteredView

class VATRegisteredViewSpec extends AmlsViewSpec with Matchers {

  lazy val vat_registered = inject[VATRegisteredView]
  lazy val fp             = inject[VATRegisteredFormProvider]

  val name = "Person Name"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "VATRegisteredView" must {

    "have correct title" in new ViewFixture {

      def view = vat_registered(fp().fill(VATRegisteredNo), true, 1, None, name)

      doc.title must startWith(messages("responsiblepeople.registeredforvat.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = vat_registered(fp().fill(VATRegisteredYes("1234")), true, 1, None, name)

      heading.html    must be(messages("responsiblepeople.registeredforvat.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

    }

    behave like pageWithErrors(
      vat_registered(
        fp().withError("registeredForVAT", "error.required.rp.registered.for.vat"),
        false,
        1,
        None,
        name
      ),
      "registeredForVAT",
      "error.required.rp.registered.for.vat"
    )

    behave like pageWithErrors(
      vat_registered(
        fp().withError("vrnNumber", "error.invalid.vat.number"),
        false,
        1,
        None,
        name
      ),
      "vrnNumber",
      "error.invalid.vat.number"
    )

    behave like pageWithBackLink(vat_registered(fp(), true, 1, None, name))
  }
}
