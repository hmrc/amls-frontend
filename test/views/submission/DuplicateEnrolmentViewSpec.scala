/*
 * Copyright 2023 HM Revenue & Customs
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

package views.submission

import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import views.Fixture
import views.html.submission.DuplicateEnrolmentView

class DuplicateEnrolmentViewSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val duplicateEnrolmentView = app.injector.instanceOf[DuplicateEnrolmentView]
    implicit val requestWithToken = addTokenForView()
    val url = "/foo"
    def view = duplicateEnrolmentView(url)
  }

  "DuplicateEnrolmentView" must {
    "display the correct title, heading and content" in new ViewFixture {

      doc.title mustBe s"${messages("error.submission.problem.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      doc.select("h1").text() mustBe messages("error.submission.problem.title")

      val link = doc.getElementById("report-link")

      link.text() mustBe messages("error.submission.duplicate_enrolment.link.text")
      link.attr("href") mustBe url

      val paragraphText = doc.getElementsByClass("govuk-body").text()
      paragraphText must include(messages("error.submission.duplicate_enrolment.content.line1"))
      paragraphText must include(messages("error.submission.duplicate_enrolment.content.line2"))
    }
  }
}
