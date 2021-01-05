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

package views.businessactivities

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessactivities.{UkAccountantsAddress, WhoIsYourAccountantIsUk, WhoIsYourAccountantName}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.businessactivities.who_is_your_accountant_is_uk_address


class who_is_your_accountant_is_ukSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    lazy val address = app.injector.instanceOf[who_is_your_accountant_is_uk_address]
    implicit val requestWithToken = addTokenForView()
  }

  val defaultName = WhoIsYourAccountantName("accountantName",Some("tradingName"))
  val defaultIsUkTrue = WhoIsYourAccountantIsUk(true)
  val defaultUkAddress = UkAccountantsAddress("line1","line2",None,None,"AB12CD")

  "who_is_your_accountant_is_uk view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[WhoIsYourAccountantIsUk] = Form2(defaultIsUkTrue)

      def view = address(form2, true, defaultName.accountantsName)

      doc.title must startWith(Messages("businessactivities.whoisyouraccountant.location.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[WhoIsYourAccountantIsUk] = Form2(defaultIsUkTrue)

      def view = address(form2, true, defaultName.accountantsName)

      heading.html must be(Messages("businessactivities.whoisyouraccountant.location.header", defaultName.accountantsName))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = address(form2, true, defaultName.accountantsName)

      errorSummary.html() must include("third not a message Key")

      doc.getElementById("isUK")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")
    }

    "have a back link" in new ViewFixture {
      def view = address(EmptyForm, true, defaultName.accountantsName)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}