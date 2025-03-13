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

import forms.responsiblepeople.CountryOfBirthFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.CountryOfBirthView

class CountryOfBirthViewSpec extends AmlsViewSpec with Matchers {

  lazy val country_of_birth = inject[CountryOfBirthView]
  lazy val fp               = inject[CountryOfBirthFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "Person Name"

  val locations: Seq[SelectItem] = Seq(
    SelectItem(text = "Country 1", value = Some("country:1"))
  )

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "CountryOfBirthView view" must {

    "have correct title, heading and subheading" in new ViewFixture {

      def view = country_of_birth(fp(), edit = true, 1, None, name, locations)

      doc.title       must startWith(messages("responsiblepeople.country.of.birth.title"))
      heading.html    must be(messages("responsiblepeople.country.of.birth.heading", "Person Name"))
      subHeading.html must include(messages("summary.responsiblepeople"))

    }

    behave like pageWithErrors(
      country_of_birth(
        fp().withError("bornInUk", "error.required.rp.select.country.of.birth"),
        false,
        1,
        None,
        name,
        locations
      ),
      "bornInUk",
      "error.required.rp.select.country.of.birth"
    )

    behave like pageWithErrors(
      country_of_birth(
        fp().withError("country", "error.required.rp.birth.country"),
        false,
        1,
        None,
        name,
        locations
      ),
      "location-autocomplete",
      "error.required.rp.birth.country"
    )
  }
}
