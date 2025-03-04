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

import forms.renewal.InvolvedInOtherDetailsFormProvider
import models.renewal.InvolvedInOtherYes
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.InvolvedInOtherDetailsView

class InvolvedInOtherDetailsViewSpec extends AmlsViewSpec with Matchers {

  lazy val involved_in_other_details                             = inject[InvolvedInOtherDetailsView]
  lazy val fp                                                    = inject[InvolvedInOtherDetailsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  val requiredTextErrorMsg     = "error.required.renewal.ba.involved.in.other.text"
  val textMaxLengthErrorMsg    = "error.invalid.maxlength.255.renewal.ba.involved.in.other"
  val basicPunctuationErrorMsg = "error.text.validation.renewal.ba.involved.in.other"

  trait ViewFixture extends Fixture

  "InvolvedInOtherDetails" must {
    "have correct title" in new ViewFixture {
      override def view: HtmlFormat.Appendable = involved_in_other_details(fp().fill(InvolvedInOtherYes("")), true)
      doc.title must startWith(messages("renewal.involvedinother.details.title"))
    }

    "have correct headings" in new ViewFixture {
      override def view: HtmlFormat.Appendable = involved_in_other_details(fp().fill(InvolvedInOtherYes("")), true)
      heading.text    must be(messages("renewal.involvedinother.details.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "have correct hint for text input" in new ViewFixture {
      override def view: HtmlFormat.Appendable = involved_in_other_details(fp().fill(InvolvedInOtherYes("")), true)
      doc.body().getElementById("details-hint").text() mustBe (messages("renewal.involvedinother.details.txtarea.hint"))
    }

    "show required error" in {
      def doc: Document =
        Jsoup.parse(involved_in_other_details(fp().withError("details", requiredTextErrorMsg), false).body)
      checkError(doc, requiredTextErrorMsg)
    }

    "show max length error" in {
      def doc: Document =
        Jsoup.parse(involved_in_other_details(fp().withError("details", textMaxLengthErrorMsg), false).body)
      checkError(doc, textMaxLengthErrorMsg)
    }

    "show basic punctuation error" in {
      def doc: Document =
        Jsoup.parse(involved_in_other_details(fp().withError("details", basicPunctuationErrorMsg), false).body)
      checkError(doc, basicPunctuationErrorMsg)
    }
  }

  private def checkError(doc: Document, expectedError: String): Unit = {
    doc.getElementsByClass("govuk-error-summary__list").first().text() must include(messages(expectedError))
    doc.getElementById(s"details-error").text()                        must include(messages(expectedError))
  }
}
