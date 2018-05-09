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

package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.ValidationError
import jto.validation.Path
import models.moneyservicebusiness.WhichCurrencies
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.FakeApplication
import utils.AmlsSpec
import views.Fixture


class which_currenciesSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  "which_currencies view" must {
    "have correct title" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, true)

      doc.title must startWith(Messages("msb.which_currencies.title") + " - " + Messages("summary.msb"))
    }

    "have correct headings" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, true)

      heading.html must be(Messages("msb.which_currencies.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "ask the user whether they deal in foreign currencies" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, true)

      Option(doc.getElementById("usesForeignCurrencies-Yes")).isDefined must be(true)
      Option(doc.getElementById("usesForeignCurrencies-No")).isDefined must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "currencies") -> Seq(ValidationError("not a message Key")),
          (Path \ "usesForeignCurrencies") -> Seq(ValidationError("second not a message Key")),
          (Path \ "WhoWillSupply") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.msb.which_currencies(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")

      doc.getElementById("currencies")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("usesForeignCurrencies")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("WhoWillSupply")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

    }
  }
}