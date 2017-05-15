/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import models.responsiblepeople.SaRegistered
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class registered_for_self_assessmentSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  val name = "Person Name"

  "registered_for_self_assessment view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, true, 0, false, name)

      doc.title must be(Messages("responsiblepeople.registeredforselfassessment.title") + " - " +
        Messages("summary.responsiblepeople") +
      " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, true, 0, false, name)

      heading.html must be(Messages("responsiblepeople.registeredforselfassessment.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, false, 0, false, name)

      doc.getElementsByAttributeValue("name", "saRegistered") must not be empty
      doc.getElementsByAttributeValue("name", "utrNumber") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "saRegistered") -> Seq(ValidationError("not a message Key")),
          (Path \ "utrNumber") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, true, 0, false, name)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

    }
  }
}