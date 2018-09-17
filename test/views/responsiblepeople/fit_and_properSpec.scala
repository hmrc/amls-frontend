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

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import org.scalatest.{MustMatchers}
import utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class fit_and_properSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "fit_and_proper view" must {
    "have correct title" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, None, "PersonName", true, false)

      doc.title must be(
        Messages("responsiblepeople.fit_and_proper.title", "PersonName")
        + " - " + Messages("summary.responsiblepeople")+
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, None, "PersonName", true, false)

      heading.html must be(Messages("responsiblepeople.fit_and_proper.heading", "PersonName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "have the correct content" when {
      "fees are being shown when phase 2 toggle is false" in new ViewFixture {

        val form2: Form2[_] = EmptyForm

        def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, None, "PersonName", true, false)

        doc.body().html() must include(Messages("responsiblepeople.fit_and_proper.text.details"))

      }
      "phase 2 content is being shown" in new ViewFixture {

        val form2: Form2[_] = EmptyForm

        def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, None, "PersonName", true, true)

        doc.body().html() must include(Messages("responsiblepeople.fit_and_proper.text.phase_2_details"))
        doc.body().html() must include(Messages("responsiblepeople.fit_and_proper.text.phase_2_details2"))

      }
      "fees are being hidden when phase 2 toggle is false" in new ViewFixture {

        val form2: Form2[_] = EmptyForm

        def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, None, "PersonName", false, false)

        doc.body().html() must include(Messages("responsiblepeople.fit_and_proper.text.details.no.fees"))
      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasAlreadyPassedFitAndProper") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, None, "PersonName", true, false)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("hasAlreadyPassedFitAndProper")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}