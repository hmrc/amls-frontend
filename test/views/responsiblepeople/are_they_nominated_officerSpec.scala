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
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class are_they_nominated_officerSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "are_they_nominated_officer view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      val name = "firstName lastName"

      def view = views.html.responsiblepeople.are_they_nominated_officer(form2, true, 1, None, name)

      doc.title must be(Messages("responsiblepeople.aretheynominatedofficer.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.aretheynominatedofficer.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isNominatedOfficer") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isNominatedOfficer") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.are_they_nominated_officer(form2, true, 1, None, "firstName lastName")

      errorSummary.html() must include("not a message Key")
    }
  }
}
