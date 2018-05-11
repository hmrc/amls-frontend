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

package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.businesscustomer.Address
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class confirm_addressSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "confirm_address view" must {
    val address = Address("#11", "some building", Some("Some street"), Some("city"), None, Country("United Kingdome","UK"))
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.confirmaddress.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.confirm_address(form2, address, 1)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.confirmaddress.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsMatchingOwnText("#11").text mustBe "#11 some building Some street city United Kingdome"
      doc.select("input[type=radio]").size mustBe 2
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "confirmAddress") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.confirm_address(form2, address, 1)

      errorSummary.html() must include("not a message Key")
    }
  }
}