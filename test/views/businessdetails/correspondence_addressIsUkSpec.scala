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

package views.businessdetails

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.autocomplete.NameValuePair
import models.businessdetails.CorrespondenceAddressIsUk
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.correspondence_address_is_uk


class correspondence_addressIsUkSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val correspondence_address_is_uk = app.injector.instanceOf[correspondence_address_is_uk]
    implicit val requestWithToken = addTokenForView()
    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "correspondence_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddressIsUk] = Form2(CorrespondenceAddressIsUk(true))

      def view = correspondence_address_is_uk(form2, true)

      doc.title must startWith(Messages("businessdetails.correspondenceaddress.isuk.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddressIsUk] = Form2(CorrespondenceAddressIsUk(true))

      def view = correspondence_address_is_uk(form2, true)

      heading.html must be(Messages("businessdetails.correspondenceaddress.isuk.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = correspondence_address_is_uk(form2, true)

      errorSummary.html() must include("second not a message Key")

      doc.getElementById("isUK")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = correspondence_address_is_uk(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}