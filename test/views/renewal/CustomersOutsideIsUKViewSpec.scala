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

import forms.renewal.CustomersOutsideIsUKFormProvider
import models.renewal.CustomersOutsideIsUK
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.CustomersOutsideIsUKView

class CustomersOutsideIsUKViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture

  lazy val customers_outside_uk_isUK                             = inject[CustomersOutsideIsUKView]
  lazy val fp                                                    = inject[CustomersOutsideIsUKFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  "CustomersOutsideIsUKView" must {
    "have correct title" in new ViewFixture {

      def view = customers_outside_uk_isUK(fp().fill(CustomersOutsideIsUK(true)), true)

      doc.title must startWith(messages("renewal.customer.outside.uk.title") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = customers_outside_uk_isUK(fp().fill(CustomersOutsideIsUK(false)), true)

      heading.html    must be(messages("renewal.customer.outside.uk.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      customers_outside_uk_isUK(fp().withError("isOutside", "error.required.ba.renewal.select.yes"), true),
      "isOutside",
      "error.required.ba.renewal.select.yes"
    )

    behave like pageWithBackLink(customers_outside_uk_isUK(fp(), true))
  }
}
