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
import models.autocomplete.NameValuePair
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class country_of_birthSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val locations = Some(Seq(
      NameValuePair("Country 1", "country:1")
    ))
  }

  "country_of_birth view" must {
    "have correct title" in new ViewFixture {

      val form2 =  EmptyForm

      def view = views.html.responsiblepeople.country_of_birth(form2, edit = true, 1, None, "Person Name", locations)

      doc.title must startWith(Messages("responsiblepeople.country.of.birth.title"))
      heading.html must be(Messages("responsiblepeople.country.of.birth.heading", "Person Name"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "bornInUk") -> Seq(ValidationError("not a message Key")),
          (Path \ "country") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.country_of_birth(form2, edit = true, 1, None, "Person Name", locations)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("bornInUk").parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("country").parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}
