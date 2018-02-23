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

package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.autocomplete.NameValuePair
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class additional_addressSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"

    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "current_address view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.additional_address(form2, true, 1, None, name, countries)

      doc.title must be(Messages("responsiblepeople.additional_address.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.additional_address.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine4") must not be empty
      doc.getElementsByAttributeValue("name", "postCode") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK4") must not be empty
      doc.getElementsByAttributeValue("name", "country") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("not a message Key 1")),
          (Path \ "addressLine1") -> Seq(ValidationError("not a message Key 2")),
          (Path \ "addressLine2") -> Seq(ValidationError("not a message Key 3")),
          (Path \ "addressLine3") -> Seq(ValidationError("not a message Key 4")),
          (Path \ "addressLine4") -> Seq(ValidationError("not a message Key 5")),
          (Path \ "postCode") -> Seq(ValidationError("not a message Key 6")),
          (Path \ "addressLineNonUK1") -> Seq(ValidationError("not a message Key 7")),
          (Path \ "addressLineNonUK2") -> Seq(ValidationError("not a message Key 8")),
          (Path \ "addressLineNonUK3") -> Seq(ValidationError("not a message Key 9")),
          (Path \ "addressLineNonUK4") -> Seq(ValidationError("not a message Key 10")),
          (Path \ "country") -> Seq(ValidationError("not a message Key 11"))
        ))

      def view = views.html.responsiblepeople.additional_address(form2, true, 1, None, name, countries)

      errorSummary.html() must include("not a message Key 1")
      errorSummary.html() must include("not a message Key 2")
      errorSummary.html() must include("not a message Key 3")
      errorSummary.html() must include("not a message Key 4")
      errorSummary.html() must include("not a message Key 5")
      errorSummary.html() must include("not a message Key 6")
      errorSummary.html() must include("not a message Key 7")
      errorSummary.html() must include("not a message Key 8")
      errorSummary.html() must include("not a message Key 9")
      errorSummary.html() must include("not a message Key 10")
      errorSummary.html() must include("not a message Key 11")
    }
  }
}
