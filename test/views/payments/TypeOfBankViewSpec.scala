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

import forms.payments.TypeOfBankFormProvider
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.payments.TypeOfBankView

class TypeOfBankViewSpec extends PlaySpec with AmlsViewSpec {

  lazy val typeOfBankView = inject[TypeOfBankView]
  lazy val fp             = inject[TypeOfBankFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val secondaryHeading                                      = "Submit application"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "TypeOfBankView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = typeOfBankView(fp(), secondaryHeading)

      doc.title       must startWith(messages("payments.typeofbank.title"))
      heading.html    must be(messages("payments.typeofbank.header"))
      subHeading.html must include(messages("submit.registration"))
    }

    "display all fields" in new ViewFixture {

      def view = typeOfBankView(fp(), secondaryHeading)

      val radios: Elements = doc.getElementsByClass("govuk-radios__input")
      radios.size() mustBe 2

      val trueRadio: Element  = radios.get(0)
      val falseRadio: Element = radios.get(1)

      trueRadio.id() mustBe "typeOfBank"
      trueRadio.`val`() mustBe "true"
      trueRadio.nextElementSibling().text() mustBe messages("lbl.yes")

      falseRadio.id() mustBe "typeOfBank-2"
      falseRadio.`val`() mustBe "false"
      falseRadio.nextElementSibling().text() mustBe messages("lbl.no")
    }

    behave like pageWithErrors(
      typeOfBankView(fp().withError("typeOfBank", "payments.typeofbank.error"), secondaryHeading),
      "typeOfBank",
      "payments.typeofbank.error"
    )

    behave like pageWithBackLink(typeOfBankView(fp(), secondaryHeading))
  }
}
