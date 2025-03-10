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

package views.payments

import forms.payments.WaysToPayFormProvider
import models.payments.WaysToPay
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.payments.WaysToPayView

class WaysToPayViewSpec extends AmlsViewSpec {

  lazy val waysToPayView = inject[WaysToPayView]
  lazy val formProvider  = inject[WaysToPayFormProvider]
  val secondaryHeading   = "Submit application"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WaysToPayView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = waysToPayView(formProvider(), secondaryHeading)

      doc.title       must startWith(messages("payments.waystopay.title"))
      heading.html    must be(messages("payments.waystopay.header"))
      subHeading.html must include(messages("submit.registration"))
      doc.html        must include(messages("payments.waystopay.info"))
      doc.html        must include(messages("payments.waystopay.info2"))
      doc.html        must include(messages("payments.waystopay.lead.time"))
    }

    "display all fields" in new ViewFixture {

      def view = waysToPayView(formProvider(), secondaryHeading)

      val radios = doc.getElementsByClass("govuk-radios__input")
      radios.size() mustBe 2

      WaysToPay.all.map(_.toString).zipWithIndex.foreach { case (field, index) =>
        val radio = radios.get(index)
        radio.id() mustBe s"waysToPay_$index"
        radio.`val`() mustBe field
        radio.nextElementSibling().text() mustBe messages(s"payments.waystopay.$field")
      }
    }

    behave like pageWithErrors(
      waysToPayView(formProvider().withError("waysToPay", "payments.waystopay.error"), secondaryHeading),
      "waysToPay",
      "payments.waystopay.error"
    )

    behave like pageWithBackLink(waysToPayView(formProvider(), secondaryHeading))
  }
}
