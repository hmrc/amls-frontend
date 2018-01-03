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

package views.businessmatching.updateservice

import forms.{EmptyForm, Form2, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class fit_and_properSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "fit_and_proper view" must {
    "have correct title" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.businessmatching.updateservice.fit_and_proper(form2, true)

      doc.title must be(
        Messages("businessmatching.updateservice.fitandproper.title")
          + " - " + Messages("summary.updateinformation") +
          " - " + Messages("title.amls") +
          " - " + Messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.businessmatching.updateservice.fit_and_proper(form2, true)

      heading.html must be(Messages("businessmatching.updateservice.fitandproper.header"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }

    "have the correct content" when {
      "fees are being shown" in new ViewFixture {

        val form2: Form2[_] = EmptyForm

        def view = views.html.businessmatching.updateservice.fit_and_proper(form2, true)

        doc.body().html() must include(Messages("businessmatching.updateservice.fitandproper.info"))

      }
      "fees are being hidden" in new ViewFixture {

        val form2: Form2[_] = EmptyForm

        def view = views.html.businessmatching.updateservice.fit_and_proper(form2, false)

        doc.body().html() must include(Messages("businessmatching.updateservice.fitandproper.info.no.fees"))
      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "passedFitAndProper") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.updateservice.fit_and_proper(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("passedFitAndProper")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }

}
