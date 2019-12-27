/*
 * Copyright 2019 HM Revenue & Customs
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
import models.Country
import models.businessactivities.{AccountantsAddress, NonUkAccountantsAddress, WhoIsYourAccountantIsUk, WhoIsYourAccountantName}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture


class who_is_your_accountant_non_uk_addressSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken = addTokenForView()
  }

  val defaultName = WhoIsYourAccountantName("accountantName",Some("tradingName"))
  val defaultIsUkTrue = WhoIsYourAccountantIsUk(true)
  val defaultNonUkAddress = NonUkAccountantsAddress("line1","line2",None,None, Country("India", "IN"))

  "who_is_your_accountant_non_uk_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AccountantsAddress] = Form2(defaultNonUkAddress)

      def view = views.html.businessactivities.who_is_your_accountant_non_uk_address(form2, true, defaultName.accountantsName, mockAutoComplete.getCountries)

      doc.title must startWith(Messages("businessactivities.whoisyouraccountant.address.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AccountantsAddress] = Form2(defaultNonUkAddress)

      def view = views.html.businessactivities.who_is_your_accountant_non_uk_address(form2, true, defaultName.accountantsName, mockAutoComplete.getCountries)

      heading.html must be(Messages("businessactivities.whoisyouraccountant.address.header", defaultName.accountantsName))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "addressLineNonUK1") -> Seq(ValidationError("fourth not a message Key")),
          (Path \ "addressLineNonUK2") -> Seq(ValidationError("fifth not a message Key")),
          (Path \ "addressLineNonUK3") -> Seq(ValidationError("sixth not a message Key")),
          (Path \ "addressLineNonUK4") -> Seq(ValidationError("seventh not a message Key")),
          (Path \ "country") -> Seq(ValidationError("ninth not a message Key"))
        ))

      def view = views.html.businessactivities.who_is_your_accountant_non_uk_address(form2, true, defaultName.accountantsName, mockAutoComplete.getCountries)

      errorSummary.html() must include("fourth not a message Key")
      errorSummary.html() must include("fifth not a message Key")
      errorSummary.html() must include("sixth not a message Key")
      errorSummary.html() must include("seventh not a message Key")
      errorSummary.html() must include("ninth not a message Key")

      doc.getElementById("addressLineNonUK1").parent
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

      doc.getElementById("addressLineNonUK2").parent
        .getElementsByClass("error-notification").first().html() must include("fifth not a message Key")

      doc.getElementById("addressLineNonUK3").parent
        .getElementsByClass("error-notification").first().html() must include("sixth not a message Key")

      doc.getElementById("addressLineNonUK4").parent
        .getElementsByClass("error-notification").first().html() must include("seventh not a message Key")

      doc.getElementById("country").parent
        .getElementsByClass("error-notification").first().html() must include("ninth not a message Key")

    }

    "have a back link" in new ViewFixture {
      def view = views.html.businessactivities.who_is_your_accountant_non_uk_address(EmptyForm, true, defaultName.accountantsName, mockAutoComplete.getCountries)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}