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

package views.responsiblepeople.address

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.autocomplete.NameValuePair
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsSpec, AmlsViewSpec}
import views.Fixture
import views.html.responsiblepeople.address.additional_address

class additional_addressSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val additional_address = app.injector.instanceOf[additional_address]
    implicit val requestWithToken = addTokenForView()

    val name = "firstName lastName"
  }

  "current_address view" must {

    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = additional_address(form2, true, 1, None, name)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      doc.title must be(Messages("responsiblepeople.additional_address.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.additional_address.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("not a message Key 1"))
        ))

      def view = additional_address(form2, true, 1, None, name)

      errorSummary.html() must include("not a message Key 1")
    }
  }
}
