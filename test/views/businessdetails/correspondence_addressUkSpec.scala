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

package views.businessdetails

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.autocomplete.NameValuePair
import models.businessdetails.CorrespondenceAddressUk
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.correspondence_address_uk


class correspondence_addressUkSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val correspondence_address_uk = app.injector.instanceOf[correspondence_address_uk]
    implicit val requestWithToken = addTokenForView()
    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "correspondence_address view" must {
    "have correct title" in new ViewFixture {
      val form2: ValidForm[CorrespondenceAddressUk] = Form2(
        CorrespondenceAddressUk(
          "Name",
          "BusinessName",
          "addressLine1",
          "addressLine1",
          None,
          None,
          "AB12CD"))

      def view = correspondence_address_uk(form2, true)

      doc.title must startWith(Messages("businessdetails.correspondenceaddress.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddressUk] = Form2(CorrespondenceAddressUk(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        "AB12CD"
      ))
      def view = correspondence_address_uk(form2, true)

      heading.html must be(Messages("businessdetails.correspondenceaddress.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "correspondenceaddress-fieldset") -> Seq(ValidationError("not a message Key")),
          (Path \ "postCode-fieldset") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = correspondence_address_uk(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("third not a message Key")

      doc.getElementById("postCode-fieldset")
        .getElementsByClass("error-notification")
        .first()
        .html() must include("not a message Key")

      doc.getElementById("postCode-fieldset")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = correspondence_address_uk(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}