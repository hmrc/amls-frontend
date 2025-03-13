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

package views.supervision

import forms.supervision.PenalisedByProfessionalFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.PenalisedByProfessionalView

class PenalisedByProfessionalViewSpec extends AmlsViewSpec with Matchers {

  lazy val viewUnderTest = inject[PenalisedByProfessionalView]
  lazy val fp            = inject[PenalisedByProfessionalFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PenalisedByProfessionalView" must {

    "have the correct title" in new ViewFixture {
      override def view = viewUnderTest(fp(), false)

      doc.title() mustBe
        s"${messages("supervision.penalisedbyprofessional.title")} - ${messages("summary.supervision")}" +
        s" - ${messages("title.amls")} - ${messages("title.gov")}"
    }

    "have the correct heading" in new ViewFixture {
      override def view = viewUnderTest(fp(), false)

      doc.getElementsByTag("h1").text() must be(messages("supervision.penalisedbyprofessional.title"))
    }

    "have the correct content" in new ViewFixture {
      override def view = viewUnderTest(fp(), false)

      Seq(
        "supervision.penalisedbyprofessional.subtitle",
        "supervision.penalisedbyprofessional.line_1",
        "supervision.penalisedbyprofessional.line_2",
        "supervision.penalisedbyprofessional.line_3",
        "supervision.penalisedbyprofessional.line_4",
        "supervision.penalisedbyprofessional.line_5",
        "supervision.penalisedbyprofessional.details"
      ) foreach { msg =>
        doc.getElementsByClass("govuk-grid-column-two-thirds").text() must include(messages(msg))
      }
    }

    "have the correct legend" in new ViewFixture {
      override def view: HtmlFormat.Appendable = viewUnderTest(fp(), false)

      doc
        .getElementsByTag("legend")
        .first()
        .text() mustBe messages("supervision.penalisedbyprofessional.heading1")
    }

    behave like pageWithErrors(
      viewUnderTest(
        fp().withError("penalised", "error.required.professionalbody.penalised.by.professional.body"),
        true
      ),
      "penalised",
      "error.required.professionalbody.penalised.by.professional.body"
    )

    behave like pageWithBackLink(viewUnderTest(fp(), false))
  }
}
