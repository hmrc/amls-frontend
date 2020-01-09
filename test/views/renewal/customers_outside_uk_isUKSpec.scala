/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.renewal.CustomersOutsideUK
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsSpec, AutoCompleteServiceMocks}
import views.Fixture

class customers_outside_uk_isUKSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks{
    implicit val requestWithToken = addToken(request)
  }

  "percentage view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CustomersOutsideUK] = Form2(CustomersOutsideUK(Option(Seq.empty[Country])))

      def view = views.html.renewal.customers_outside_uk_isUK(form2, true)

      doc.title must startWith(Messages("renewal.customer.outside.uk.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CustomersOutsideUK] = Form2(CustomersOutsideUK(Option(Seq.empty[Country])))

      def view = views.html.renewal.customers_outside_uk_isUK(form2, true)

      heading.html must be(Messages("renewal.customer.outside.uk.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isOutside") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.customers_outside_uk_isUK(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("isOutside")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      val form2: ValidForm[CustomersOutsideUK] = Form2(CustomersOutsideUK(Option(Seq.empty[Country])))

      def view = views.html.renewal.customers_outside_uk_isUK(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}