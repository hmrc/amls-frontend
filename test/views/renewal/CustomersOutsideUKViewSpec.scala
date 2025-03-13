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

package views.renewal

import forms.renewal.CustomersOutsideUKFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.renewal.CustomersOutsideUKView

class CustomersOutsideUKViewSpec extends AmlsViewSpec with Matchers with AutoCompleteServiceMocks {

  trait ViewFixture extends Fixture

  lazy val customers_outside_uk                                  = inject[CustomersOutsideUKView]
  lazy val fp                                                    = inject[CustomersOutsideUKFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  "percentage view" must {
    "have correct title" in new ViewFixture {

      def view = customers_outside_uk(fp(), true, mockAutoComplete.formOptions)

      doc.title must startWith(
        messages("renewal.customer.outside.uk.countries.title") + " - " + messages("summary.renewal")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = customers_outside_uk(fp(), true, mockAutoComplete.formOptions)

      heading.html    must be(messages("renewal.customer.outside.uk.countries.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "have correct text and hint" in new ViewFixture {

      def view = customers_outside_uk(fp(), true, mockAutoComplete.formOptions)

      doc.getElementsByClass("govuk-body").first.text must be(messages("renewal.customer.outside.uk.countries.text"))
      doc.getElementsByClass("govuk-hint").first.text must be(
        messages("businessactivities.high.value.customer.lbl.hint")
      )
    }

    behave like pageWithErrors(
      customers_outside_uk(
        fp().withError("countries", "error.required.renewal.customer.country.name"),
        true,
        mockAutoComplete.formOptions
      ),
      "location-autocomplete-0",
      "error.required.renewal.customer.country.name"
    )

    behave like pageWithBackLink(customers_outside_uk(fp(), true, mockAutoComplete.formOptions))
  }
}
