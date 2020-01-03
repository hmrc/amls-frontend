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

package views.businessdetails

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.autocomplete.NameValuePair
import models.businessdetails.CorrespondenceAddressNonUk
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture


class correspondence_addressNonUkSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }


  "correspondence_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddressNonUk] = Form2(CorrespondenceAddressNonUk(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        Country("AB12CD", "XX")
      ))

      def view = views.html.businessdetails.correspondence_address_non_uk(form2, true, countries)

      doc.title must startWith(Messages("businessdetails.correspondenceaddress.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddressNonUk] = Form2(CorrespondenceAddressNonUk(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        Country("Antarctica", "XX")
      ))
      def view = views.html.businessdetails.correspondence_address_non_uk(form2, true, countries)

      heading.html must be(Messages("businessdetails.correspondenceaddress.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "country-fieldset") -> Seq(ValidationError("fourth not a message Key"))
        ))

      def view = views.html.businessdetails.correspondence_address_non_uk(form2, true, countries)

      errorSummary.html() must include("fourth not a message Key")

      val test = doc.getElementById("country-fieldset")
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = views.html.businessdetails.correspondence_address_non_uk(form2, true, countries)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}