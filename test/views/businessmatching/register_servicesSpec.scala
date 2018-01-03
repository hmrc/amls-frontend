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

package views.businessmatching

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, BusinessActivities}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class register_servicesSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "register_services view" must {
    "have correct title" when {
      "pre-submission" in new ViewFixture {

        val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(AccountancyServices)))

        def view = views.html.businessmatching.register_services(form2, edit = true, Set("01"), Set.empty, isPreSubmission = true)

        doc.title must startWith(Messages("businessmatching.registerservices.title") + " - " + Messages("summary.businessmatching"))
        heading.html must be(Messages("businessmatching.registerservices.title"))
        subHeading.html must include(Messages("summary.businessmatching"))

      }
      "post-submission" in new ViewFixture {

        val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(AccountancyServices)))

        def view = views.html.businessmatching.register_services(form2, edit = true, Set("01"), Set.empty, isPreSubmission = false)

        doc.title must startWith(Messages("businessmatching.registerservices.other.title") + " - " + Messages("summary.businessmatching"))
        heading.html must be(Messages("businessmatching.registerservices.other.title"))
        subHeading.html must include(Messages("summary.businessmatching"))

      }
    }

    "notify of services already selected" when {
      "status is post submission" in new ViewFixture {

        def view = views.html.businessmatching.register_services(EmptyForm, edit = true, Set("01"), Set.empty, isPreSubmission = false)

        html must include(Messages("businessmatching.registerservices.existing"))

      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "businessActivities") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.register_services(form2, edit = true, Set("01"), Set.empty, isPreSubmission = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("businessActivities")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
    "hide the return to progress link" in new ViewFixture {
      def view = views.html.businessmatching.register_services(EmptyForm, edit = true, Set("01"), Set.empty, isPreSubmission = true, showReturnLink = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}