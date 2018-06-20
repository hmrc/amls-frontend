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

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.{BusinessMatchingMsbServices, TransmittingMoney, BusinessMatchingMsbService}
import org.scalatest.{MustMatchers}
import  utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class servicesSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "services view" must {
    "have correct title in when in presubmission mode " in new ViewFixture {

      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))

      def view = views.html.businessmatching.services(form2, edit = true, isPreSubmission = true)

      doc.title must startWith(Messages("msb.services.title") + " - " + Messages("summary.businessmatching"))
      heading.html must be(Messages("msb.services.title"))
      subHeading.html must include(Messages("summary.businessmatching"))
    }

    "have correct title in when in non-presubmission mode " in new ViewFixture {

      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))

      def view = views.html.businessmatching.services(form2, edit = true, isPreSubmission = false)

      doc.title must startWith(Messages("msb.services.title") + " - " + Messages("summary.updateinformation"))
      heading.html must be(Messages("msb.services.title"))
      subHeading.html must include(Messages("summary.updateinformation"))
    }


    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "msbServices") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.services(form2, edit = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("msbServices")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
    "hide the return to progress link"in new ViewFixture {
      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))

      def view = views.html.businessmatching.services(form2, edit = true, showReturnLink = false)
      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}