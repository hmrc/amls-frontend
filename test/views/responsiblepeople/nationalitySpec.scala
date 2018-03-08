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
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.autocomplete.NameValuePair
import play.api.i18n.Messages
import views.Fixture

class nationalitySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val countries = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "nationality view" must {
      "have correct title, headings and form fields" in new ViewFixture {
        val form2 = EmptyForm
        def view = views.html.responsiblepeople.nationality(form2, true, 1, None, "firstName lastName", countries)

        doc.title must be(Messages("responsiblepeople.nationality.title") +
          " - " + Messages("summary.responsiblepeople") +
          " - " + Messages("title.amls") +
          " - " + Messages("title.gov"))
        heading.html must be(Messages("responsiblepeople.nationality.heading", "firstName lastName"))
        subHeading.html must include(Messages("summary.responsiblepeople"))
    }

    "show errors in the correct locations" in new ViewFixture {
        val form2: InvalidForm = InvalidForm (Map.empty,
          Seq (
          (Path \ "nationality") -> Seq (ValidationError ("not a message Key") ),
          (Path \ "otherCountry") -> Seq (ValidationError ("second not a message Key") )
          )
        )

        def view = views.html.responsiblepeople.nationality(form2, true, 1, None, "firstName lastName", countries)
        errorSummary.html () must include ("not a message Key")
        errorSummary.html () must include ("second not a message Key")
      }
  }
}