/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import models.businessmatching.CompanyRegistrationNumber
import org.scalatest.{MustMatchers}
import  utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class company_registration_numberSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "company_registration_number view" must {
    "have correct title for pre-submission mode" in new ViewFixture {

      val form2: ValidForm[CompanyRegistrationNumber] = Form2(CompanyRegistrationNumber("12345678"))

      def view = views.html.businessmatching.company_registration_number(form2, edit = false, isPreSubmission = true)

      doc.title must startWith(Messages("businessmatching.registrationnumber.title") + " - " + Messages("summary.businessmatching"))
      heading.html must be(Messages("businessmatching.registrationnumber.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "have correct title for non  pre-submission mode" in new ViewFixture {

      val form2: ValidForm[CompanyRegistrationNumber] = Form2(CompanyRegistrationNumber("12345678"))

      def view = views.html.businessmatching.company_registration_number(form2, edit = true, isPreSubmission = false)

      doc.title must startWith(Messages("businessmatching.registrationnumber.title") + " - " + Messages("summary.updateinformation"))
      heading.html must be(Messages("businessmatching.registrationnumber.title"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }


    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.company_registration_number(form2, edit = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("companyRegistrationNumber")
        .parent
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "hide the return to progress link when requested" in new ViewFixture {
      val form2: ValidForm[CompanyRegistrationNumber] = Form2(CompanyRegistrationNumber("12345678"))

      def view = views.html.businessmatching.company_registration_number(form2, edit = true, showReturnLink = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}