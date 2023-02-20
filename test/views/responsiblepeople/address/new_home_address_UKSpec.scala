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

package views.responsiblepeople.address

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.new_home_address_UK

class new_home_address_UKSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture {
    lazy val new_home_address_UK = app.injector.instanceOf[new_home_address_UK]
    implicit val requestWithToken = addTokenForView()

    val name = "firstName lastName"
  }

  "new_home_address_UK view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = new_home_address_UK(form2, 1, name)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      doc.title must be(Messages("responsiblepeople.new.home.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.new.home.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine4") must not be empty
      doc.getElementsByAttributeValue("name", "postCode") must not be empty
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("not a message Key 1")),
          (Path \ "addressLine1") -> Seq(ValidationError("not a message Key 2")),
          (Path \ "addressLine2") -> Seq(ValidationError("not a message Key 3")),
          (Path \ "addressLine3") -> Seq(ValidationError("not a message Key 4")),
          (Path \ "addressLine4") -> Seq(ValidationError("not a message Key 5")),
          (Path \ "postCode") -> Seq(ValidationError("not a message Key 6"))
        ))

      def view = new_home_address_UK(form2, 1, name)

      errorSummary.html() must include("not a message Key 1")
      errorSummary.html() must include("not a message Key 2")
      errorSummary.html() must include("not a message Key 3")
      errorSummary.html() must include("not a message Key 4")
      errorSummary.html() must include("not a message Key 5")
      errorSummary.html() must include("not a message Key 6")
    }
  }
}
