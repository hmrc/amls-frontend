/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.{UsesForeignCurrencies, UsesForeignCurrenciesYes, WhichCurrencies}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.uses_foreign_currencies


class uses_foreign_currenciesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val uses_foreign_currencies = app.injector.instanceOf[uses_foreign_currencies]
    implicit val requestWithToken = addTokenForView()
  }

  "uses foreign currencies view" must {
    "have correct title" in new ViewFixture {

      val formData: ValidForm[UsesForeignCurrencies] = Form2(UsesForeignCurrenciesYes)

      def view = uses_foreign_currencies(formData, true)

      doc.title must startWith(Messages("renewal.msb.foreign_currencies.header") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val formData: ValidForm[UsesForeignCurrencies] = Form2(UsesForeignCurrenciesYes)

      def view = uses_foreign_currencies(formData, true)

      heading.html must be(Messages("renewal.msb.foreign_currencies.header"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "ask the user whether they deal in foreign currencies" in new ViewFixture {

      val formData: ValidForm[UsesForeignCurrencies] = Form2(UsesForeignCurrenciesYes)

      def view = uses_foreign_currencies(formData, true)

      Option(doc.getElementById("usesForeignCurrencies-true")).isDefined must be(true)
      Option(doc.getElementById("usesForeignCurrencies-false")).isDefined must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "usesForeignCurrencies") -> Seq(ValidationError("seventh not a message Key"))
        ))

      def view = uses_foreign_currencies(form2, true)

      doc.getElementById("usesForeignCurrencies")
        .getElementsByClass("error-notification").first().html() must include("seventh not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = uses_foreign_currencies(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}