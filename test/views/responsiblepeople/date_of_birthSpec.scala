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

package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.responsiblepeople.date_of_birth

class date_of_birthSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val date_of_birth = app.injector.instanceOf[date_of_birth]
    implicit val requestWithToken = addTokenForView()
  }

  "date_of_birth view" must {

    "have a back link" in new ViewFixture {
      val form2 = EmptyForm
      def view = date_of_birth(form2, false, 1, None, "Gary")
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = date_of_birth(form2, false, 1, None, "Gary")

      doc.title() must startWith(Messages("responsiblepeople.date.of.birth.title") + " - " + Messages("summary.responsiblepeople"))

    }

    "have correct heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = date_of_birth(form2, false, 1, None, "first last")

      heading.html() must be(Messages("responsiblepeople.date.of.birth.heading", "first last"))
    }

    "show errors in correct places when validation fails and have the correct fields" in new ViewFixture {

      val messageKey1 = "definitely not a message key"
      val dateField = "dateOfBirth"

      val form2: InvalidForm = InvalidForm(
        Map("thing" -> Seq("thing")),
        Seq((Path \ dateField, Seq(ValidationError(messageKey1))))
      )

      def view = date_of_birth(form2, false, 1, None, "first last")

      errorSummary.html() must include(messageKey1)

      doc.getElementById(dateField).html() must include(messageKey1)

      doc.getElementsByAttributeValue("name", "dateOfBirth.day") must not be empty
      doc.getElementsByAttributeValue("name", "dateOfBirth.month") must not be empty
      doc.getElementsByAttributeValue("name", "dateOfBirth.year") must not be empty
    }
  }
}
