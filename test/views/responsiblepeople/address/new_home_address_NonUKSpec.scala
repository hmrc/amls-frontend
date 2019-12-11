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

package views.responsiblepeople.address

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.autocomplete.NameValuePair
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class new_home_address_NonUKSpec extends AmlsSpec {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"

    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "new_home_address_NonUK view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.address.new_home_address_NonUK(form2, 1, name, countries)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      doc.title must be(Messages("responsiblepeople.new.home.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.new.home.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
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
          (Path \ "addressLineNonUK1") -> Seq(ValidationError("not a message Key 7")),
          (Path \ "addressLineNonUK2") -> Seq(ValidationError("not a message Key 8")),
          (Path \ "addressLineNonUK3") -> Seq(ValidationError("not a message Key 9")),
          (Path \ "addressLineNonUK4") -> Seq(ValidationError("not a message Key 10")),
          (Path \ "country") -> Seq(ValidationError("not a message Key 11"))
        ))

      def view = views.html.responsiblepeople.address.new_home_address_NonUK(form2, 1, name, countries)

      errorSummary.html() must include("not a message Key 1")
      errorSummary.html() must include("not a message Key 7")
      errorSummary.html() must include("not a message Key 8")
      errorSummary.html() must include("not a message Key 9")
      errorSummary.html() must include("not a message Key 10")
      errorSummary.html() must include("not a message Key 11")
    }
  }
}
