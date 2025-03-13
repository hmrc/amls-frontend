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

package views.businessactivities

import forms.businessactivities.IdentifySuspiciousActivityFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.IdentifySuspiciousActivityView

class IdentifySuspiciousActivityViewSpec extends AmlsViewSpec with Matchers {

  lazy val activity: IdentifySuspiciousActivityView             = inject[IdentifySuspiciousActivityView]
  lazy val formProvider: IdentifySuspiciousActivityFormProvider = inject[IdentifySuspiciousActivityFormProvider]

  implicit val request: Request[_] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "Spec view" must {
    "have correct title" in new ViewFixture {

      def view = activity(formProvider(), true)

      doc.title must be(
        messages("businessactivities.identify-suspicious-activity.title") + " - " +
          messages("summary.businessactivities") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = activity(formProvider(), true)

      heading.html    must be(messages("businessactivities.identify-suspicious-activity.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      def view = activity(formProvider(), true)

      doc.getElementsByAttributeValue("name", "hasWrittenGuidance") must not be empty

    }

    behave like pageWithErrors(
      activity(formProvider().bind(Map("hasWrittenGuidance" -> "")), false),
      "hasWrittenGuidance",
      "error.required.ba.suspicious.activity"
    )

    behave like pageWithBackLink(activity(formProvider(), true))
  }
}
