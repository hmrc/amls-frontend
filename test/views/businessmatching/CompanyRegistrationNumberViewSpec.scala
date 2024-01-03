/*
 * Copyright 2024 HM Revenue & Customs
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

import forms.businessmatching.CompanyRegistrationNumberFormProvider
import models.businessmatching.CompanyRegistrationNumber
import org.scalatest.MustMatchers
import play.api.data.Form
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.CompanyRegistrationNumberView


class CompanyRegistrationNumberViewSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val viewUnderTest = app.injector.instanceOf[CompanyRegistrationNumberView]
    implicit val requestWithToken = addTokenForView()
  }

  val formProvider = new CompanyRegistrationNumberFormProvider()()
  val formWithData: Form[CompanyRegistrationNumber] = formProvider.fill(CompanyRegistrationNumber("12345678"))

  "CompanyRegistrationNumberView view" must {
    "have correct title for pre-submission mode" in new ViewFixture {

      def view = viewUnderTest(formWithData, edit = false, isPreSubmission = true)

      doc.title must startWith(messages("businessmatching.registrationnumber.title") + " - " + messages("summary.businessmatching"))
      heading.html must include(messages("businessmatching.registrationnumber.title"))
      caption.html must include(messages("summary.businessmatching"))

    }

    "have correct title for non pre-submission mode" in new ViewFixture {

      def view = viewUnderTest(formWithData, edit = true, isPreSubmission = false)

      doc.title must startWith(messages("businessmatching.registrationnumber.title") + " - " + messages("summary.updateinformation"))
      heading.html must include(messages("businessmatching.registrationnumber.title"))
      caption.html must include(messages("summary.updateinformation"))

    }


    "show errors in the correct locations" in new ViewFixture {

      val errorMessage = messages("error.invalid.bm.registration.number.allowed")

      val formWithInvalidData: Form[CompanyRegistrationNumber] = formProvider.bind(Map("value" -> "12345£"))

      def view = viewUnderTest(formWithInvalidData, edit = true)

      doc.getElementsByClass("govuk-error-summary").first.hasText mustBe true

      doc.getElementById("value-error").hasText mustBe true

    }

    "hide the return to progress link when requested" in new ViewFixture {

      def view = viewUnderTest(formWithData, edit = true, showReturnLink = false)

      doc.body().text() must not include messages("link.return.registration.progress")
    }

    "have a back link in pre-submission mode" in new ViewFixture {
      def view = viewUnderTest(formProvider, edit = false, isPreSubmission = true)

      doc.getElementById("back-link").attr("href") mustBe "javascript:history.back()"
    }

    "have a back link in non pre-submission mode" in new ViewFixture {
      def view = viewUnderTest(formProvider, edit = false, isPreSubmission = false)

      doc.getElementById("back-link").attr("href") mustBe "javascript:history.back()"
    }

  }
}