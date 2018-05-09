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
import models.businessmatching.BusinessType
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class person_registeredSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"
  }

  "person_registered view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.person_registered(form2, 1, None)

      doc.title must be(Messages("responsiblepeople.person.registered.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))

      heading.html must be(Messages("responsiblepeople.person.registered.title", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "registerAnotherPerson") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registerAnotherPerson") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.person_registered(form2, 1, None)

      errorSummary.html() must include("not a message Key")
    }
  }
}
