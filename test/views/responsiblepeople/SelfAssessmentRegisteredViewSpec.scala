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

package views.responsiblepeople

import forms.responsiblepeople.SelfAssessmentRegisteredFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.SelfAssessmentRegisteredView

class SelfAssessmentRegisteredViewSpec extends AmlsViewSpec with Matchers {

  lazy val registeredView = inject[SelfAssessmentRegisteredView]
  lazy val fp             = inject[SelfAssessmentRegisteredFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val name = "Person Name"

  "SelfAssessmentRegisteredView view" must {

    "have correct title" in new ViewFixture {

      def view = registeredView(fp(), true, 0, None, name)

      doc.title must be(
        messages("responsiblepeople.registeredforselfassessment.title") + " - " +
          messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = registeredView(fp(), true, 0, None, name)

      heading.html    must be(messages("responsiblepeople.registeredforselfassessment.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "have correct form fields" in new ViewFixture {

      def view = registeredView(fp(), false, 0, None, name)

      doc.getElementsByAttributeValue("name", "saRegistered") must not be empty
      doc.getElementsByAttributeValue("name", "utrNumber")    must not be empty
    }

    behave like pageWithErrors(
      registeredView(fp().withError("saRegistered", "error.required.sa.registration"), false, 0, None, name),
      "saRegistered",
      "error.required.sa.registration"
    )

    behave like pageWithErrors(
      registeredView(fp().withError("utrNumber", "error.required.utr.number"), false, 0, None, name),
      "utrNumber",
      "error.required.utr.number"
    )

    behave like pageWithBackLink(registeredView(fp(), false, 0, None, name))
  }
}
