/*
 * Copyright 2022 HM Revenue & Customs
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
import views.html.responsiblepeople.address.new_home_address

class new_home_addressSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture {
    lazy val new_home_address = app.injector.instanceOf[new_home_address]
    implicit val requestWithToken = addTokenForView()

    val name = "firstName lastName"
  }

  "new_home_address view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = new_home_address(form2, 1, name)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      doc.title must be(Messages("responsiblepeople.new.home.is.uk.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.new.home.is.uk.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("not a message Key 1"))
        ))

      def view = new_home_address(form2, 1, name)

      errorSummary.html() must include("not a message Key 1")
    }
  }
}
