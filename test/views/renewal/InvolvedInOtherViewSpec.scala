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
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.InvolvedInOtherView

class InvolvedInOtherViewSpec extends AmlsViewSpec with Matchers {

  lazy val involved_in_other: InvolvedInOtherView = inject[InvolvedInOtherView]
  lazy val fp: InvolvedInOtherFormProvider = inject[InvolvedInOtherFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "InvolvedInOtherView" must {
    "have correct title" in new ViewFixture {

      def view: HtmlFormat.Appendable = involved_in_other(fp().fill(false), edit = true, None)

      doc.title must startWith(messages("renewal.involvedinother.title"))
    }

    "have correct headings" in new ViewFixture {

      def view: HtmlFormat.Appendable = involved_in_other(fp().fill(false), edit = true, None)

      heading.html must be(messages("renewal.involvedinother.title"))
      subHeading.html must include(messages("summary.renewal"))

    }

    "correctly list business activities" in new ViewFixture {

      def view: HtmlFormat.Appendable = involved_in_other(fp().fill(false), edit = true, Some(List("test activities string")))

      html must include(messages("businessactivities.confirm-activities.subtitle_4"))
      html must include("test activities string")
    }

    behave like pageWithErrors(
      involved_in_other(
        fp().withError("involvedInOther", "error.required.renewal.ba.involved.in.other"), edit = false, None
      ),
      "involvedInOther",
      "error.required.renewal.ba.involved.in.other"
    )

    behave like pageWithBackLink(involved_in_other(fp(), edit = false, None))
  }
}