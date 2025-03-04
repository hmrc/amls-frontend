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

import forms.responsiblepeople.NationalityFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.NationalityView

class NationalityViewSpec extends AmlsViewSpec with Matchers {

  lazy val nationality = inject[NationalityView]
  lazy val fp          = inject[NationalityFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  val countries = Seq(
    SelectItem(Some("country:1"), "Country 1")
  )

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "NationalityView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = nationality(fp(), true, 1, None, name, countries)

      doc.title       must be(
        messages("responsiblepeople.nationality.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("responsiblepeople.nationality.heading", "firstName lastName"))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    behave like pageWithErrors(
      nationality(
        fp().withError("nationality", "error.required.nationality"),
        false,
        1,
        None,
        name,
        countries
      ),
      "nationality",
      "error.required.nationality"
    )

    behave like pageWithErrors(
      nationality(
        fp().withError("country", "error.invalid.rp.nationality.country"),
        false,
        1,
        None,
        name,
        countries
      ),
      "location-autocomplete",
      "error.invalid.rp.nationality.country"
    )

    behave like pageWithBackLink(nationality(fp(), true, 1, None, name, countries))
  }
}
