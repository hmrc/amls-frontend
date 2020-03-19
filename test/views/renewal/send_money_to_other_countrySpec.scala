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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.SendMoneyToOtherCountry
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture


class send_money_to_other_countrySpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "send_money_to_other_country view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = views.html.renewal.send_money_to_other_country(form2, true)

      doc.title must be(Messages("renewal.msb.send.money.title") +
        " - " + Messages("summary.renewal") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = views.html.renewal.send_money_to_other_country(form2, true)

      heading.html must be(Messages("renewal.msb.send.money.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "money") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.send_money_to_other_country(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("money")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "show the correct return progress link" in new ViewFixture {
      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = views.html.renewal.send_money_to_other_country(form2, true)
      doc.getElementsByClass("return-link").first().text() must include(Messages("link.return.renewal.progress"))
      doc.getElementsByClass("return-link").first().html() must include(controllers.renewal.routes.RenewalProgressController.get.url)
    }

    "have a back link" in new ViewFixture {

      def view = views.html.renewal.send_money_to_other_country(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}