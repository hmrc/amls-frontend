/*
 * Copyright 2024 HM Revenue & Customs
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

import forms.businessactivities.NCARegisteredFormProvider
import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.NCARegisteredView


class NCARegisteredViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val registered: NCARegisteredView = inject[NCARegisteredView]
  lazy val formProvider: NCARegisteredFormProvider = inject[NCARegisteredFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "nca_registered view" must {
    "have correct title" in new ViewFixture {

      def view = registered(formProvider(), true)

      doc.title must be(messages("businessactivities.ncaRegistered.title") + " - " +
        messages("summary.businessactivities") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = registered(formProvider(), true)

      heading.html must be(messages("businessactivities.ncaRegistered.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      def view = registered(formProvider(), true)

      doc.getElementsByAttributeValue("name", "ncaRegistered") must not be empty

    }

    behave like pageWithErrors(
      registered(formProvider().bind(Map("ncaRegistered" -> "")), true),
      "ncaRegistered",
      "error.required.ba.select.nca"
    )

    behave like pageWithBackLink(registered(formProvider(), false))
  }
}