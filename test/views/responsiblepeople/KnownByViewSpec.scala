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

import forms.responsiblepeople.KnownByFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.KnownByView

class KnownByViewSpec extends AmlsViewSpec with Matchers {

  lazy val known_by: KnownByView   = inject[KnownByView]
  lazy val fp: KnownByFormProvider = inject[KnownByFormProvider]

  val name = "firstName lastName"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "KnownByView view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view: HtmlFormat.Appendable = known_by(fp(), edit = true, 1, None, name)

      doc.title       must startWith(Messages("responsiblepeople.knownby.title"))
      heading.html    must be(Messages("responsiblepeople.knownby.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "hasOtherNames") must not be empty
      doc.getElementsByAttributeValue("name", "otherNames")    must not be empty
    }

    behave like pageWithErrors(
      known_by(fp().withError("hasOtherNames", "error.required.rp.hasOtherNames"), edit = false, 1, None, name),
      "hasOtherNames",
      "error.required.rp.hasOtherNames"
    )

    behave like pageWithErrors(
      known_by(fp().withError("otherNames", "error.invalid.rp.char"), edit = false, 1, None, name),
      "otherNames",
      "error.invalid.rp.char"
    )

    behave like pageWithBackLink(known_by(fp(), edit = false, 1, None, name))
  }
}
