/*
 * Copyright 2021 HM Revenue & Customs
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
import jto.validation.{Path, ValidationError}
import models.moneyservicebusiness.SendMoneyToOtherCountry
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.send_money_to_other_country


class send_money_to_other_countrySpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val send_money_to_other_country = app.injector.instanceOf[send_money_to_other_country]
    implicit val requestWithToken = addTokenForView()
  }

  "branches_or_agents view" must {

    "have the back link button" in new ViewFixture {
      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))
      def view = send_money_to_other_country(form2, true)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = send_money_to_other_country(form2, true)

      doc.title must be(Messages("msb.send.money.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = send_money_to_other_country(form2, true)

      heading.html must be(Messages("msb.send.money.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "money") -> Seq(ValidationError("not a message Key"))
        ))

      def view = send_money_to_other_country(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("money")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}