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

package views.renewal

import forms.renewal.InvolvedInOtherFormProvider
import models.renewal.InvolvedInOtherNo
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.InvolvedInOtherView

class InvolvedInOtherViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val involved_in_other = inject[InvolvedInOtherView]
  lazy val fp = inject[InvolvedInOtherFormProvider]
  implicit val requestWithToken = addTokenForView()

  trait ViewFixture extends Fixture

  "InvolvedInOtherView" must {
    "have correct title" in new ViewFixture {

      def view = involved_in_other(fp().fill(InvolvedInOtherNo), true, None)

      doc.title must startWith(messages("renewal.involvedinother.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = involved_in_other(fp().fill(InvolvedInOtherNo), true, None)

      heading.html must be(messages("renewal.involvedinother.title"))
      subHeading.html must include(messages("summary.renewal"))

    }

    "correctly list business activities" in new ViewFixture {

      def view = involved_in_other(fp().fill(InvolvedInOtherNo), true, Some(List("test activities string")))

      html must include(messages("businessactivities.confirm-activities.subtitle_4"))
      html must include("test activities string")
    }

    behave like pageWithErrors(
      involved_in_other(
        fp().withError("involvedInOther", "error.required.renewal.ba.involved.in.other"), false, None
      ),
      "involvedInOther",
      "error.required.renewal.ba.involved.in.other"
    )

    behave like pageWithErrors(
      involved_in_other(
        fp().withError("details", "error.text.validation.renewal.ba.involved.in.other"), false, None
      ),
      "details",
      "error.text.validation.renewal.ba.involved.in.other"
    )

    behave like pageWithBackLink(involved_in_other(fp(), false, None))
  }
}