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

import forms.responsiblepeople.address.AdditionalAddressNonUKFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.AdditionalAddressNonUKView

class AdditionalAddressNonUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val nonUKView = inject[AdditionalAddressNonUKView]
  lazy val fp        = inject[AdditionalAddressNonUKFormProvider]

  val name = "firstName lastName"

  val countries = Seq(
    SelectItem(Some("ES"), "Spain")
  )

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AdditionalAddressNonUKView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = nonUKView(fp(), true, 1, None, name, countries)

      doc.title       must be(
        messages("responsiblepeople.additional_address_country.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("responsiblepeople.additional_address_country.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "addressLine1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine4") must not be empty
      doc.getElementsByAttributeValue("name", "country")      must not be empty

    }

    Seq(1, 2, 3, 4) foreach { line =>
      behave like pageWithErrors(
        nonUKView(
          fp().withError(s"addressLine$line", s"error.text.validation.address.line$line"),
          false,
          1,
          None,
          name,
          countries
        ),
        s"addressLine$line",
        s"error.text.validation.address.line$line"
      )
    }

    behave like pageWithErrors(
      nonUKView(
        fp().withError("country", "error.invalid.country"),
        false,
        1,
        None,
        name,
        countries
      ),
      "location-autocomplete",
      "error.invalid.country"
    )

    behave like pageWithBackLink(nonUKView(fp(), false, 1, None, name, countries))
  }
}
