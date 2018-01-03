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

package views.changeofficer

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.{VATRegistered, VATRegisteredYes}
import models.changeofficer.StillEmployed
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class remove_responsible_personSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_responsible_person view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.changeofficer.remove_responsible_person(EmptyForm, "testName")

      doc.title must startWith(Messages("changeofficer.removeresponsibleperson.title") + " - " + Messages("summary.updateinformation"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.changeofficer.remove_responsible_person(EmptyForm, "testName")

      heading.html must be(Messages("changeofficer.removeresponsibleperson.heading", "testName"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val messageKey1 = "definitely not a message key"
      val dateField = "date"

      val form2: InvalidForm = InvalidForm(
        Map("thing" -> Seq("thing")),
        Seq((Path \ dateField, Seq(ValidationError(messageKey1))))
      )

      def view = views.html.changeofficer.remove_responsible_person(form2, "first last")

      errorSummary.html() must include(messageKey1)

      doc.getElementById(dateField).html() must include(messageKey1)

      doc.getElementsByAttributeValue("name", "date.day") must not be empty
      doc.getElementsByAttributeValue("name", "date.month") must not be empty
      doc.getElementsByAttributeValue("name", "date.year") must not be empty
    }

  }
}
