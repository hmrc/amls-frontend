/*
 * Copyright 2018 HM Revenue & Customs
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
import models.renewal.WhichCurrencies
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.FakeApplication
import utils.GenericTestHelper
import views.Fixture
import views.html.renewal.which_currencies


class which_currenciesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  "which_currencies view" must {
    "have correct title" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = which_currencies(formData, true)

      doc.title must startWith(Messages("renewal.msb.whichcurrencies.header") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = which_currencies(formData, true)

      heading.html must be(Messages("renewal.msb.whichcurrencies.header"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "ask the user whether they deal in foreign currencies" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = which_currencies(formData, true)

      Option(doc.getElementById("usesForeignCurrencies-Yes")).isDefined must be(true)
      Option(doc.getElementById("usesForeignCurrencies-No")).isDefined must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "currencies") -> Seq(ValidationError("not a message Key")),
          (Path \ "bankNames") -> Seq(ValidationError("third not a message Key")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("fifth not a message Key")),
          (Path \ "usesForeignCurrencies") -> Seq(ValidationError("seventh not a message Key"))
        ))

      def view = which_currencies(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fifth not a message Key")

      doc.getElementById("currencies")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("usesForeignCurrency-fieldset")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

      doc.getElementById("usesForeignCurrency-fieldset")
        .getElementsByClass("form-field--error").first().nextElementSibling().nextElementSibling()
        .getElementsByClass("error-notification").first().html()must include("fifth not a message Key")

      doc.getElementById("usesForeignCurrencies")
        .getElementsByClass("error-notification").first().html() must include("seventh not a message Key")

    }
  }
}